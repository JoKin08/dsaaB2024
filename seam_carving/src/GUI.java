import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Random;

public class GUI {
    public static volatile int SLIDER = 1000;
    public static final int SEAM_COLOR = new Color(88, 150, 236).getRGB();
    public static String ICONS_FOLDER = "Icons";
    public static final int ICON_SIZE = 30;
    public static final EnergyType ENERGY_TYPE = EnergyType.BACKWARD; // Seam Carving的能量类型：前进或后退
    public static final boolean CROP_SNAPSHOT = false;

    private int brushWidth; // 确定用于标记优先级掩码的画笔的宽度
    private boolean carving;
    private boolean recording;
    private boolean update;
    private boolean grayscale;
    private boolean direction; // Seam的方向：Removing = False, Adding = True.
    private boolean highlight;
    private boolean horizontal; // 水平（True）或垂直（False）Seam Carver
    private int count; // 用于保存快照的计数器
    private int scaleW, scaleH; // 显示图像的缩放比例
    private final JLabel displayImage; // 用于显示图像的JLabel
    private final SeamCarver[] carver; // 用于存储Seam Carver的数组
    private int idx; // 用于指示当前Seam Carver的索引
    private final SeamCarverFactory factory; // 用于创建Seam Carver
    private BufferedImage bufferedImage; // 用于显示图像的缓冲图像

    public GUI() {
        this.carver = new SeamCarver[] { null, null };
        this.factory = new SeamCarverFactory();
        this.update = true;
        this.grayscale = false;

        JFrame frame = new JFrame("DSAAB Group22");
        JPanel containerPanel = new JPanel(new BorderLayout());
        JPanel menuPanel = new JPanel(new FlowLayout()); // menuPanel 存储所有按钮、复选框和滑块
        JPanel panel = new JPanel(new FlowLayout());
        JPanel controlPanel = new JPanel(new FlowLayout());

        containerPanel.add(menuPanel, BorderLayout.SOUTH);
        containerPanel.add(panel, BorderLayout.NORTH);
        containerPanel.add(controlPanel, BorderLayout.CENTER);

        containerPanel.setBackground(new Color(255, 255, 224));
        panel.setOpaque(false);
        menuPanel.setOpaque(false);
        controlPanel.setOpaque(false);

        // 添加显示图像，显示carving的图像
        this.displayImage = new JLabel(icon("dragdrop.png", ICON_SIZE / 4),
                JLabel.CENTER);
        panel.add(this.displayImage);


        // 添加展示dragdrop图像的区域
        this.addDropTarget(frame, menuPanel);
        this.addMouseListener();

        // 显示我们的logo和title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setOpaque(false);
        JLabel title = new JLabel(icon("logo.png", ICON_SIZE * 3 / 4),
                JLabel.CENTER);
        titlePanel.add(title);
        controlPanel.add(titlePanel);

        // 添加滑动条
        JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        sliderPanel.setOpaque(false);
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, SLIDER, SLIDER / 2);
        slider.setFocusable(false);
        ImageIcon[] speeds = new ImageIcon[] {
                icon("speed1.png"),
                icon("speed2.png"),
                icon("speed3.png")
        };
        JLabel speedometer = new JLabel(speeds[1], JLabel.CENTER);
        slider.addChangeListener(e -> speedometer.setIcon(speeds[slider.getValue() /
                (SLIDER / speeds.length + 1)]));
        sliderPanel.add(speedometer);
        sliderPanel.add(slider);
        controlPanel.add(sliderPanel);

        // 添加“显示接缝”、“水平”、“记录”、“灰度”复选框
        // 显示接缝
        Font font = new Font("Arial", Font.BOLD, 15);
        JPanel checkBoxPanel = new JPanel(new GridLayout(1, 4));
        checkBoxPanel.setOpaque(false);
        JCheckBox highlightCheckBox = new JCheckBox("Show Seams");
        highlightCheckBox.setOpaque(false);
        highlightCheckBox.setFont(font);
        highlightCheckBox.addItemListener(e -> {
            this.highlight = !this.highlight;
            this.carver[this.idx].updateImage(this.highlight, SEAM_COLOR);
            this.updateDisplayImage();
        });
        checkBoxPanel.add(highlightCheckBox);
        // 水平
        JCheckBox horizontalCheckBox = new JCheckBox("Horizontal");
        horizontalCheckBox.setOpaque(false);
        horizontalCheckBox.setFont(font);
        horizontalCheckBox.addItemListener(e -> {
            this.horizontal = !this.horizontal;
            this.idx = this.horizontal ? 1 : 0;
            this.clearBufferedImage();
            if (this.update)
                this.updateDisplayImage();
            SeamCarver carver = this.carver[this.idx];
            frame.setTitle("Seam-Carving - " + carver.getWidth() + " x " +
                    carver.getHeight());
        });
        checkBoxPanel.add(horizontalCheckBox);
        // 记录
        JCheckBox recordingCheckBox = new JCheckBox("Recording");
        recordingCheckBox.setOpaque(false);
        recordingCheckBox.setFont(font);
        recordingCheckBox.addItemListener(e -> this.recording = !this.recording);
        checkBoxPanel.add(recordingCheckBox);
        // 灰度
        JCheckBox grayscaleCheckBox = new JCheckBox("Grayscale");
        grayscaleCheckBox.setOpaque(false);
        grayscaleCheckBox.setFont(font);
        grayscaleCheckBox.addItemListener(e -> {
            this.grayscale = !this.grayscale;
            this.clearBufferedImage();
            if (this.update)
                this.updateDisplayImage();
            frame.setTitle("Seam-Carving - " + "grayscale");
        });
        checkBoxPanel.add(grayscaleCheckBox);

        this.addKeyListeners(
                new AbstractButton[] { highlightCheckBox, horizontalCheckBox,
                        recordingCheckBox, grayscaleCheckBox },
                new int[] { KeyEvent.VK_S, KeyEvent.VK_H, KeyEvent.VK_R, KeyEvent.VK_U });

        JPanel spacer1 = new JPanel();
        spacer1.setPreferredSize(new Dimension(100, 20)); 
        spacer1.setOpaque(false);
        menuPanel.add(checkBoxPanel);
        menuPanel.add(spacer1);

        // 添加“暂停/播放”、“添加”、“删除”、“快照”按钮
        JPanel buttonPanel = new JPanel(new GridLayout(1, 5));
        buttonPanel.setOpaque(false);
        ImageIcon play = icon("play.png");
        ImageIcon pause = icon("pause.png");
        JButton playButton = new JButton("Animate Seams");
        playButton.setOpaque(false);
        playButton.setIcon(play);
        JButton addButton = new JButton("Add Seam");
        addButton.setOpaque(false);
        addButton.setIcon(icon("add.png"));
        JButton removeButton = new JButton("Remove Seam");
        removeButton.setOpaque(false);
        removeButton.setIcon(icon("remove.png"));
        JButton snapshotButton = new JButton("Snapshot");
        snapshotButton.setOpaque(false);
        snapshotButton.setIcon(icon("snapshot.png"));

        this.addKeyListeners(
                new AbstractButton[] { playButton, addButton, removeButton, snapshotButton },
                new int[] { KeyEvent.VK_SPACE, KeyEvent.VK_RIGHT, KeyEvent.VK_LEFT,
                        KeyEvent.VK_C });

        // 按下“播放”按钮时运行的函数
        Runnable animate = () -> {
            // 一直carve图像，直到图像完全消失
             // 然后通过移除的seam重建图像
             // 持续重复直到用户停止carve
            while (this.carving) {
                if (this.direction) {
                    this.carveAdd(frame, slider);
                    this.carveRemove(frame, slider);
                } else {
                    this.carveRemove(frame, slider);
                    this.carveAdd(frame, slider);
                }
            }
        };

        Thread[] thread = { new Thread(animate) };
        // 单击“播放/暂停”按钮时
        playButton.addActionListener(e -> {
            addButton.setEnabled(this.carving);
            removeButton.setEnabled(this.carving);
            snapshotButton.setEnabled(this.carving);
            highlightCheckBox.setEnabled(this.carving);
            grayscaleCheckBox.setEnabled(this.carving);
            recordingCheckBox.setEnabled(this.carving);
            horizontalCheckBox.setEnabled(this.carving);
            this.carving = !this.carving;
            if (this.carving) {
                playButton.setIcon(pause);
                thread[0].start();
            } else {
                playButton.setIcon(play);
                thread[0] = new Thread(animate);
            }
        });
        // 单击“添加”按钮时
        addButton.addActionListener(e -> {
            this.direction = true;
            SeamCarver carver = this.carver[this.idx];
            boolean valid = carver.add(this.highlight, SEAM_COLOR);
            if (valid) {
                if (this.recording)
                    captureSnapshot();
                if (this.update)
                    this.updateDisplayImage();
                frame.setTitle("Karve - " + carver.getWidth() + " x " + carver.getHeight());
            }
        });
        // 单击“删除”按钮时
        removeButton.addActionListener(e -> {
            this.direction = false;
            SeamCarver carver = this.carver[this.idx];
            boolean valid = carver.remove(this.highlight, SEAM_COLOR);
            if (valid) {
                if (this.recording)
                    captureSnapshot();
                if (this.update)
                    this.updateDisplayImage();
                frame.setTitle("Karve - " + carver.getWidth() + " x " + carver.getHeight());
            }
        });
        // 单击“快照”按钮时
        snapshotButton.addActionListener(e -> {
            if (!this.recording)
                captureSnapshot();
        });
        buttonPanel.add(playButton);
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(snapshotButton);

        JPanel spacer2 = new JPanel();
        spacer2.setPreferredSize(new Dimension(100, 40)); 
        spacer2.setOpaque(false);

        menuPanel.add(buttonPanel);
        menuPanel.add(spacer2);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        this.setEnabled(menuPanel, false);

        frame.add(containerPanel);
        frame.pack();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }

    // 添加seam
    private void carveAdd(JFrame frame, JSlider slider) {
        SeamCarver carver = this.carver[this.idx];
        while (this.carving && carver.add(this.highlight, SEAM_COLOR)) {
            if (this.recording)
                captureSnapshot();
            if (this.update)
                this.updateDisplayImage();
            frame.setTitle("Karve - " + carver.getWidth() + " x " + carver.getHeight());
            Utils.delay(SLIDER - slider.getValue());
        }
    }

    // 移除seam
    private void carveRemove(JFrame frame, JSlider slider) {
        SeamCarver carver = this.carver[this.idx];
        while (this.carving && carver.remove(this.highlight, SEAM_COLOR)) {
            if (this.recording)
                captureSnapshot();
            if (this.update)
                this.updateDisplayImage();
            frame.setTitle("Karve - " + carver.getWidth() + " x " + carver.getHeight());
            Utils.delay(SLIDER - slider.getValue());
        }
    }

    // 将以一维数组表示的像素转换为二维数组
    private int[][] convertTo2D(int[] pixels, int width, int height) {
        int[][] result = new int[height][width];
    
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                result[i][j] = pixels[i * width + j];
            }
        }
    
        return result;
    } 

    // 清除缓冲图像
    private void clearBufferedImage() {
        for (int y = 0; y < this.bufferedImage.getHeight(); y++) {
            for (int x = 0; x < this.bufferedImage.getWidth(); x++) {
                this.bufferedImage.setRGB(x, y, 0xFF);
            }
        }
    }

    // 更新显示图像
    private void updateDisplayImage() {
        ImageIcon icon = this.updateBufferedImage();
        this.displayImage.setIcon(icon);
    }

    // 设置组件是否可用
    private void setEnabled(JPanel panel, boolean enable) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JPanel) {
                setEnabled((JPanel) comp, enable);
            } else {
                comp.setEnabled(enable);
            }
        }
    }

    // 添加拖放目标
    private void addDropTarget(JFrame frame, JPanel menuPanel) {
        this.displayImage.setDropTarget(new DropTarget() {
            public synchronized void drop(DropTargetDropEvent evt) {
                if (carving)
                    return;
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List) evt
                            .getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    File image = droppedFiles.get(0);
                    // 创建水平/竖直Seam Carver
                    carver[0] = factory.create(image, false, ENERGY_TYPE);
                    carver[1] = factory.create(image, true, ENERGY_TYPE);

                    int width = carver[0].getWidth();
                    int height = carver[0].getHeight();
                    brushWidth = Utils.max(Utils.min(width, height) / 120, 5);

                    bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    bufferedImage.setAccelerationPriority(1f);

                    idx = horizontal ? 1 : 0;
                    int scale = Utils.getDimensions(carver[idx].getWidth(), carver[idx].getHeight());
                    scaleW = width / scale;
                    scaleH = height / scale;

                    ImageIcon icon = updateBufferedImage();
                    displayImage.setIcon(icon);

                    setEnabled(menuPanel, true);
                    frame.pack();
                    frame.setTitle("Group22 - " + carver[idx].getWidth() + " x " + carver[idx].getHeight());

                    evt.dropComplete(true);
                } catch (Exception ignored) {
                    evt.dropComplete(false);
                }
            }
        });
    }

    // 添加键盘监听器
    private void addKeyListeners(AbstractButton[] buttons, int[] keyCodes) {
        for (int i = 0; i < buttons.length; i++) {
            AbstractButton button = buttons[i];
            button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).clear();
            button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCodes[i], 0),
                    "KEY_BINDING");
            button.getActionMap().put("KEY_BINDING", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    button.doClick();
                }
            });
        }
    }

    // 添加鼠标监听器
    private void addMouseListener() {
        this.displayImage.addMouseMotionListener(new MouseAdapter() {
            Random rand = new Random(System.currentTimeMillis());

            @Override
            public void mouseDragged(MouseEvent e) {
                if (carving || !update || carver[idx] == null)
                    return;
                float x = e.getX(), y = e.getY();
                SeamCarver current = carver[idx];
                int imageWidth = bufferedImage.getWidth(), imageHeight = bufferedImage.getHeight();
                float labelStepW = (float) imageWidth / displayImage.getWidth();
                float labelStepH = (float) imageHeight / displayImage.getHeight();
                int cX = (int) (x * labelStepW + 0.5f); 
                int cY = (int) (y * labelStepH + 0.5f); 
                if (horizontal) {
                    int temp1 = cX;
                    cX = cY;
                    cY = temp1;
                    cY = imageWidth - cY;

                    int temp2 = imageWidth;
                    imageWidth = imageHeight;
                    imageHeight = temp2;
                }
                if (cX >= imageWidth || cY >= imageHeight)
                    return;
                boolean isLeftClick = SwingUtilities.isLeftMouseButton(e);
                int energy = isLeftClick ? 0 : (ENERGY_TYPE == EnergyType.FORWARD ? rand.nextInt(256) : 255);
                int color = isLeftClick ? Color.RED.getRGB() : Color.GREEN.getRGB();
                int[] image = current.getImage();
                int cWidth = current.getWidth(), cHeight = current.getHeight();
                for (int r = Utils.max(cY - brushWidth, 0); r < Utils.min(cY + brushWidth, cHeight); r++) {
                    for (int c = Utils.max(cX - brushWidth, 0); c < Utils.min(cX + brushWidth, cWidth); c++) {
                        current.setEnergy(c, r, energy);
                        image[r * cWidth + c] = color;
                    }
                }
                updateDisplayImage();
            }
        });
    }

    // 更新缓冲图像
    private ImageIcon updateBufferedImage() {
        SeamCarver carver = this.carver[this.idx];

        int width = carver.getWidth();
        int height = carver.getHeight();

        int[] pixels = carver.getImage();

        Utils.parallel((cpu, cpus) -> {
            if (this.horizontal && this.grayscale) {
                // 同时应用水平和灰度处理
                int[][] grayPixels = Utils.grayscale(this.convertTo2D(pixels, width, height));
                for (int y = height - 1 - cpu; y >= 0; y -= cpus) {
                    for (int x = 0; x < width; x++) {
                        int grayValue = grayPixels[y][x];
                        int grayPixel = (0xFF << 24) | (grayValue << 16) | (grayValue << 8) | grayValue;
                        this.bufferedImage.setRGB(height - 1 - y, x, grayPixel);
                    }
                }
                for (int y = cpu; y < height; y += cpus) {
                    this.bufferedImage.setRGB(y, width - 1, 0xFF);
                }
            } else if (this.horizontal) {
                // 仅应用水平处理
                for (int y = height - 1 - cpu; y >= 0; y -= cpus) {
                    for (int x = 0; x < width; x++) {
                        this.bufferedImage.setRGB(height - 1 - y, x, pixels[y * width + x]);
                    }
                }
                for (int y = cpu; y < height; y += cpus) {
                    this.bufferedImage.setRGB(y, width - 1, 0xFF);
                }
            } else if (this.grayscale) {
                // 仅应用灰度处理
                int[][] grayPixels = Utils.grayscale(this.convertTo2D(pixels, width, height));
                for (int y = cpu; y < height; y+= cpus) {
                    for (int x = 0; x < width; x++) {
                        int grayValue = grayPixels[y][x];
                        int grayPixel = (0xFF << 24) | (grayValue << 16) | (grayValue << 8) | grayValue;
                        this.bufferedImage.setRGB(x, y, grayPixel);
                    }
                }
                for (int y = cpu; y < height; y += cpus) {
                    this.bufferedImage.setRGB(width - 1, y, 0xFF);
                }
            } else { // 无处理
                for (int y = cpu; y < height; y += cpus) {
                    for (int x = 0; x < width; x++) {
                        this.bufferedImage.setRGB(x, y, pixels[y * width + x]);
                    }
                }
                for (int y = cpu; y < height; y += cpus) {
                    this.bufferedImage.setRGB(width - 1, y, 0xFF);
                }
            }
        });

        return new ImageIcon(this.bufferedImage.getScaledInstance(
                Utils.max(this.scaleW, 1),
                Utils.max(this.scaleH, 1),
                Image.SCALE_FAST));
    }

    // 捕获快照
    private void captureSnapshot() {
        SeamCarver carver = this.carver[this.idx];
        String filename = Utils.joinPath(Main.SNAPSHOTS_DIR, "Snapshot" + this.count++ + ".png");
        if (CROP_SNAPSHOT) {
            Utils.writeImage(
                    carver.getImage(),
                    carver.getWidth(),
                    carver.getHeight(),
                    this.horizontal,
                    filename);
        } else {
            File snapshot = new File(filename);
            try {
                ImageIO.write(this.bufferedImage, "PNG", snapshot);
            } catch (IOException ignored) {
            }
        }

    }

    // 获取icon
    private ImageIcon icon(String filename, int... dims) {
        URL url = getClass().getResource(ICONS_FOLDER + "/" + filename);
        ImageIcon icon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(url));
        int width, height;
        if (dims.length == 0) {
            width = height = ICON_SIZE;
        } else {
            width = icon.getIconWidth() / dims[0];
            height = icon.getIconHeight() / dims[0];
        }
        return new ImageIcon(icon.getImage().getScaledInstance(width, height, Image.SCALE_FAST));
    }
}
