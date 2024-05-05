/*
 * GUI
 * Alex Eidt
 * Runs the Graphical User Interface (GUI) window that allows the user
 * to interface with the Karver.
 */

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;

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
    // Determines the range (0 - SLIDER) of values for the slider.
    public static volatile int SLIDER = 1000;
    // Color of the seams (if highlighting).
    public static final int SEAM_COLOR = new Color(88, 150, 236).getRGB();
    // File path to the Icons folder.
    public static String ICONS_FOLDER = "Icons";
    // Size of the button icons.
    public static final int ICON_SIZE = 30;
    // Energy Type to use for Seam Carving. Either Forward or Backward.
    public static final EnergyType ENERGY_TYPE = EnergyType.BACKWARD;
    // If true, crop snapshots, otherwise all snapshots have same size.
    public static final boolean CROP_SNAPSHOT = false;

    // Determines the width of the "brush" used to mark the priority mask by
    // clicking on the image.
    private int brushWidth;
    // Flag storing whether the carving animation is happening.
    private boolean carving;
    // Flag storing whether the carving is being recorded.
    private boolean recording;
    // Flag storing whether the display image should be updated.
    private boolean update;
    // Flag storing whether the display image should be grayscale.
    private boolean grayscale;
    // The direction the carving animation plays. Removing -> False, Adding -> True.
    private boolean direction;
    // Flag that determines whether removed/added seams should be colored.
    private boolean highlight;
    // Flag that determines whether horizontal or vertical seam carving happens.
    private boolean horizontal;
    // Track frame numbers when recording or taking snapshots.
    private int count;
    // Scaling factors for display image.
    private int scaleW, scaleH;
    // The display image.
    private final JLabel displayImage;
    // Stores the vertical and horizontal Seam Carvers.
    private final SeamCarver[] carver;
    // The index of the current Seam Carver in "this.carver".
    private int idx;
    // Create Seam Carvers.
    private final SeamCarverFactory factory;
    // Buffered Image for display.
    private BufferedImage bufferedImage;

    public GUI() {
        this.carver = new SeamCarver[] { null, null };
        this.factory = new SeamCarverFactory();
        this.update = true;
        this.grayscale = false;

        JFrame frame = new JFrame("DSAAB Group22");
        JPanel containerPanel = new JPanel(new BorderLayout());
        JPanel menuPanel = new JPanel(new FlowLayout());
        JPanel panel = new JPanel(new FlowLayout());
        JPanel controlPanel = new JPanel(new FlowLayout());

        containerPanel.add(menuPanel, BorderLayout.SOUTH);
        containerPanel.add(panel, BorderLayout.NORTH);
        containerPanel.add(controlPanel, BorderLayout.CENTER);

        containerPanel.setBackground(new Color(255, 255, 224));
        panel.setOpaque(false);
        menuPanel.setOpaque(false);
        controlPanel.setOpaque(false);

        // Add the display image showing the image being carved.
        this.displayImage = new JLabel(icon("dragdrop.png", ICON_SIZE / 4),
                JLabel.CENTER);
        panel.add(this.displayImage);

        // The menuPanel stores all the buttons, checkboxes and the slider.

        // Add File Drag and Drop to the Image Label.
        this.addDropTarget(frame, menuPanel);
        // Add Priority Masking with Mouse Clicking to Image Label.
        this.addMouseListener();

        // Add the "Karve" logo.
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setOpaque(false);
        JLabel title = new JLabel(icon("logo.png", ICON_SIZE * 3 / 4),
                JLabel.CENTER);
        // Add "title" to a JPanel to center it.
        titlePanel.add(title);
        controlPanel.add(titlePanel);

        // Add the slider.
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

        // Add the checkboxes for "Show Seams", "Horizontal", and "Record".
        // "Show Seams" checkbox.
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
        // "Horizontal" checkbox.
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
        // "Recording" checkbox.
        JCheckBox recordingCheckBox = new JCheckBox("Recording");
        recordingCheckBox.setOpaque(false);
        recordingCheckBox.setFont(font);
        recordingCheckBox.addItemListener(e -> this.recording = !this.recording);
        checkBoxPanel.add(recordingCheckBox);
        // "Grayscale" checkbox.
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
        spacer1.setPreferredSize(new Dimension(100, 20)); // 设置首选大小
        spacer1.setOpaque(false); // 设置为透明，使得背景不显示
        menuPanel.add(checkBoxPanel);
        menuPanel.add(spacer1);

        // Add all "Pause/Play", "Add", "Remove" and "Snapshot" buttons.
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

        // Function to run in separate thread when the "Play" button is pressed.
        Runnable animate = () -> {
            // Carve the image all the way until nothing is left.
            // Then begin reconstructing the image by the seams that were removed
            // and repeat until the user stops carving.
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
        // Manage animation thread when "Play/Pause" button is clicked.
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
        // Add seam back when "Add" button is clicked.
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
        // Remove seam when "Remove" button is clicked.
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
        // Create a snapshot of the current image when the "Snapshot" button is
        snapshotButton.addActionListener(e -> {
            if (!this.recording)
                captureSnapshot();
        });
        buttonPanel.add(playButton);
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(snapshotButton);

        JPanel spacer2 = new JPanel();
        spacer2.setPreferredSize(new Dimension(100, 40)); // 设置首选大小
        spacer2.setOpaque(false); // 设置为透明，使得背景不显示

        menuPanel.add(buttonPanel);
        menuPanel.add(spacer2);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        this.setEnabled(menuPanel, false);

        // panel.add(menuPanel);

        frame.add(containerPanel);
        frame.pack();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }

    // public GUI() {
    // this.carver = new SeamCarver[] { null, null };
    // this.factory = new SeamCarverFactory();
    // this.update = true;
    // this.grayscale = false;

    // JFrame frame = new JFrame("DSAAB Group22");
    // frame.setLayout(new BorderLayout()); // 设置整体布局为BorderLayout

    // // 添加展示图像区域
    // JPanel displayPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    // this.displayImage = new JLabel(icon("dragdrop.png", ICON_SIZE / 4),
    // JLabel.CENTER);
    // displayPanel.add(this.displayImage);
    // frame.add(displayPanel, BorderLayout.NORTH); // 把拖拽区域放在顶部

    // // 控制元素面板，包含logo、滑动条、复选框和按钮
    // JPanel controlPanel = new JPanel();
    // controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS)); //
    // 使用垂直布局

    // // 添加Logo
    // JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    // JLabel title = new JLabel(icon("logo.png", ICON_SIZE * 3 / 4),
    // JLabel.CENTER);
    // titlePanel.add(title);
    // controlPanel.add(titlePanel);

    // // 添加滑动条
    // JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    // JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, SLIDER, SLIDER / 2);
    // slider.setFocusable(false);
    // ImageIcon[] speeds = { icon("speed1.png"), icon("speed2.png"),
    // icon("speed3.png") };
    // JLabel speedometer = new JLabel(speeds[1], JLabel.CENTER);
    // slider.addChangeListener(e -> speedometer.setIcon(speeds[slider.getValue() /
    // (SLIDER / speeds.length + 1)]));
    // sliderPanel.add(speedometer);
    // sliderPanel.add(slider);
    // controlPanel.add(sliderPanel);

    // // 添加复选框
    // JPanel checkBoxPanel = new JPanel(new GridLayout(2, 2));
    // Font font = new Font("Arial", Font.BOLD, 15);
    // JCheckBox highlightCheckBox = new JCheckBox("Show Seams");
    // highlightCheckBox.setFont(font);
    // highlightCheckBox.addItemListener(e -> {
    // this.highlight = !this.highlight;
    // this.carver[this.idx].updateImage(this.highlight, SEAM_COLOR);
    // this.updateDisplayImage();
    // });
    // checkBoxPanel.add(highlightCheckBox);
    // JCheckBox horizontalCheckBox = new JCheckBox("Horizontal");
    // horizontalCheckBox.setFont(font);
    // horizontalCheckBox.addItemListener(e -> {
    // this.horizontal = !this.horizontal;
    // this.idx = this.horizontal ? 1 : 0;
    // this.clearBufferedImage();
    // if (this.update)
    // this.updateDisplayImage();
    // SeamCarver carver = this.carver[this.idx];
    // frame.setTitle("Seam-Carving - " + carver.getWidth() + " x " +
    // carver.getHeight());
    // });
    // checkBoxPanel.add(horizontalCheckBox);
    // JCheckBox recordingCheckBox = new JCheckBox("Recording");
    // recordingCheckBox.setFont(font);
    // recordingCheckBox.addItemListener(e -> this.recording = !this.recording);
    // checkBoxPanel.add(recordingCheckBox);
    // JCheckBox grayscaleCheckBox = new JCheckBox("Grayscale");
    // grayscaleCheckBox.setFont(font);
    // grayscaleCheckBox.addItemListener(e -> {
    // this.grayscale = !this.grayscale;
    // this.clearBufferedImage();
    // if (this.update)
    // this.updateDisplayImage();
    // frame.setTitle("Seam-Carving - " + "grayscale");
    // });
    // checkBoxPanel.add(grayscaleCheckBox);
    // controlPanel.add(checkBoxPanel);

    // // 添加按钮
    // JPanel buttonPanel = new JPanel(new GridLayout(4, 1));
    // JButton playButton = new JButton("Animate Seams", icon("play.png"));
    // JButton addButton = new JButton("Add Seam", icon("add.png"));
    // JButton removeButton = new JButton("Remove Seam", icon("remove.png"));
    // JButton snapshotButton = new JButton("Snapshot", icon("snapshot.png"));
    // buttonPanel.add(playButton);
    // buttonPanel.add(addButton);
    // buttonPanel.add(removeButton);
    // buttonPanel.add(snapshotButton);
    // controlPanel.add(buttonPanel);

    // frame.add(controlPanel, BorderLayout.CENTER); // 将控制元素面板添加到中部

    // // 设置窗口属性
    // this.addDropTarget(frame, displayPanel);
    // frame.pack();
    // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    // frame.setLocationRelativeTo(null);
    // frame.setResizable(false);
    // frame.setVisible(true);
    // }

    private void addCheckBox(String label, Font font, JPanel panel) {
        JCheckBox checkBox = new JCheckBox(label);
        checkBox.setFont(font);
        checkBox.addItemListener(e -> {
            // 根据复选框的种类更新状态
            switch (label) {
                case "Show Seams":
                    this.highlight = !this.highlight;
                    break;
                case "Horizontal":
                    this.horizontal = !this.horizontal;
                    break;
                case "Recording":
                    this.recording = !this.recording;
                    break;
                case "Grayscale":
                    this.grayscale = !this.grayscale;
                    break;
            }
            this.updateDisplayImage(); // 根据需要更新显示
        });
        panel.add(checkBox);
    }

    private void addControlButton(String text, ImageIcon icon, JPanel panel) {
        JButton button = new JButton(text);
        button.setIcon(icon);
        panel.add(button);
    }

    /*
     * Add Seams back to the image when using the animate feature.
     *
     * @param frame The UI Window. The title is updated to reflect the current size
     * of the image.
     * 
     * @param slider The slider used to determine animation speed.
     */
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

    /*
     * Remove Seams from the image when using the animate feature.
     *
     * @param frame The UI Window. The title is updated to reflect the current size
     * of the image.
     * 
     * @param slider The slider used to determine animation speed.
     */
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

    /*
     * Clears the display image by making all pixels transparent.
     */
    private void clearBufferedImage() {
        for (int y = 0; y < this.bufferedImage.getHeight(); y++) {
            for (int x = 0; x < this.bufferedImage.getWidth(); x++) {
                this.bufferedImage.setRGB(x, y, 0xFF);
            }
        }
    }

    /*
     * Sets/Updates the display image to the current state of the selected
     * Seam Carver.
     */
    private void updateDisplayImage() {
        ImageIcon icon = this.updateBufferedImage();
        this.displayImage.setIcon(icon);
    }

    /*
     * Enables/Disables all components in a JPanel.
     *
     * @param panel The panel to enable/disable.
     * 
     * @param enable If true, enable all elements, otherwise disable.
     */
    private void setEnabled(JPanel panel, boolean enable) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JPanel) {
                setEnabled((JPanel) comp, enable);
            } else {
                comp.setEnabled(enable);
            }
        }
    }

    /*
     * Adds a drop target to the display image. This allows the user to drag and
     * drop
     * files into the panel and have them be processed.
     * Code from:
     * https://stackoverflow.com/questions/811248/how-can-i-use-drag-and-drop-in-
     * swing-to-get-file-path
     *
     * @param frame The current window frame. Used to update the title.
     * 
     * @param menuPanel The menuPanel to enable once the user drops in an image.
     */
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

                    // Vertical Seam Carver
                    carver[0] = factory.create(image, false, ENERGY_TYPE);
                    // Horizontal Seam Carver
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

    /*
     * Adds Key Bindings to Buttons.
     *
     * @param buttons A list of buttons to add key bindings to.
     * 
     * @param keyCodes A list of Key Codes that each button should map to.
     */
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

    /*
     * Adds a mouse listener to the display image which allows the user to
     * mark high and low priority areas on the image for carving.
     */
    private void addMouseListener() {
        // Change pixels values of sobel image to change where seams appear.
        // Change pixels by clicking on the image.
        this.displayImage.addMouseMotionListener(new MouseAdapter() {
            /*
             * Activates whenever the user clicks and drags their mouse over any part
             * of the display image. The coordinates the user click on are converted into
             * pixel coordinates corresponding to the actual image. Depending on
             * a left or right-click the corresponding pixels are colored red or green
             * to show areas of low or high priority.
             */
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
                int cX = (int) (x * labelStepW + 0.5f); // X coordinate on actual image.
                int cY = (int) (y * labelStepH + 0.5f); // Y coordinate on actual image.
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
                        // If left click, remove edge at given coordinate. If right click, add edge.
                        current.setEnergy(c, r, energy);
                        image[r * cWidth + c] = color;
                    }
                }
                updateDisplayImage();
            }
        });
    }

    /*
     * Updates the display image for the UI.
     *
     * @return An ImageIcon representing the scaled image.
     */
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
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int grayValue = grayPixels[y][x];
                        int grayPixel = (0xFF << 24) | (grayValue << 16) | (grayValue << 8) | grayValue;
                        this.bufferedImage.setRGB(x, y, grayPixel);
                    }
                }
            } else {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        this.bufferedImage.setRGB(x, y, pixels[y * width + x]);
                    }
                }
            }
        });

        return new ImageIcon(this.bufferedImage.getScaledInstance(
                Utils.max(this.scaleW, 1),
                Utils.max(this.scaleH, 1),
                Image.SCALE_FAST));
    }

    /*
     * Captures the current image and saves to a PNG file in the "Snapshots"
     * directory.
     *
     * @return See "Snapshots" directory.
     */
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

    /*
     * Creates Image Icons for components used in the User Interface.
     *
     * @param filename The filename/path of the Icon to use.
     * 
     * @param dims The scaling factors to use to resize the Icon.
     * 
     * @return An ImageIcon scaled to the given dimensions.
     */
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
