import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// 基本的Seam Carving操作，由所有子类继承
public abstract class SeamCarverBase {
    protected int height;
    protected int width;
    protected boolean update;
    protected Stack<int[]> seams;
    protected Stack<int[]> values; // 存储从图像中删除的seam的值
    protected Stack<int[]> energyValues; // 存储从内部能量图像中移除的seam的值
    protected List<List<Integer>> energy; // 能量图像
    protected List<List<Integer>> image; // 存储实际图像的二维数组
    protected int[] data; // 存储当前图像的一维数组
    protected int[][] map; // 存储能量图像的二维数组
    protected int maxWidth; // 最大宽度

    // 构造函数接受 2D 图像数组
    public SeamCarverBase(int[][] image) {
        this.height = image.length;
        this.width = image[0].length;
        this.maxWidth = this.width * 2; // 将最大宽度设置为原始宽度的两倍
        this.update = true;
        this.seams = new Stack<>();
        this.values = new Stack<>();
        this.energyValues = new Stack<>();
        this.image = new ArrayList<>(this.height);
        this.data = new int[this.height * this.maxWidth]; // 初始化为最大宽度
        this.map = new int[this.height][this.maxWidth]; // 初始化为最大宽度
        this.energy = new ArrayList<>(this.height);

        for (int h = 0; h < this.height; h++) {
            this.image.add(new ArrayList<>(this.maxWidth)); // 初始化为最大宽度
            this.energy.add(new ArrayList<>(this.maxWidth)); // 初始化为最大宽度
        }

        for (int h = 0; h < this.height; h++) {
            for (int w = 0; w < this.width; w++) {
                this.image.get(h).add(image[h][w]);
                this.data[h * this.maxWidth + w] = image[h][w];
                this.energy.get(h).add(0); // 初始化能量图像
            }
            // 初始化剩余部分为0
            for (int w = this.width; w < this.maxWidth; w++) {
                this.image.get(h).add(0);
                this.energy.get(h).add(0);
            }
        }
    }

    public int getWidth(boolean isAdd) {
        if (isAdd)
            return this.maxWidth;
        else {
            return this.width;
        }
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    // 返回当前图像
    public int[] getImage() {
        return this.data;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    // 设置给定位置的能量值
    public void setEnergy(int x, int y, int val) {
        this.energy.get(y).set(x, val);
    }

    // 添加count个seam
    public int add(int count, boolean highlight, int color) {
        if (count <= 0)
            return 0;
        int added = 0;
        for (int i = 0; i < count; i++) {
            if (this.add(highlight, color)) {
                added++;
            } else {
                break;
            }
        }
        return added;
    }

    // 添加下一个seam
    public boolean add(boolean highlight, int color) {
        int[] path = new int[this.height];
        int[] values = new int[this.height];
        int[] energyValues = new int[this.height];

        // 找到能量最小的路径
        int maxIndex = Utils.argmax(this.map[0], this.width);
        path[0] = maxIndex;
        values[0] = this.image.get(0).get(maxIndex);
        energyValues[0] = this.energy.get(0).get(maxIndex);

        for (int h = 1; h < this.height; h++) {
            int[] row = this.map[h];
            if (maxIndex == 0) {
                maxIndex = Utils.max(row[0], row[1]) == row[0] ? 0 : 1;
            } else if (maxIndex == this.width - 1) {
                int maxValue = Utils.max(row[this.width - 2], row[this.width - 1]);
                maxIndex = row[this.width - 2] == maxValue ? this.width - 2 : this.width - 1;
            } else {
                int maxValue = Utils.max(row[maxIndex - 1], row[maxIndex], row[maxIndex + 1]);
                if (row[maxIndex - 1] == maxValue)
                    maxIndex = maxIndex - 1;
                else if (row[maxIndex + 1] == maxValue)
                    maxIndex = maxIndex + 1;
            }
            path[h] = maxIndex;
            values[h] = this.image.get(h).get(maxIndex);
            energyValues[h] = this.energy.get(h).get(maxIndex);
        }

        // // 串行添加新路径
        // for (int h = 0; h < this.height; h++) {
        // this.image.get(h).add(path[h], values[h]);
        // this.energy.get(h).add(path[h], energyValues[h]);
        // }
        // 并行添加新路径
        Utils.parallel((cpu, cpus) -> {
            for (int h = cpu; h < this.height; h += cpus) {
                // 检查是否需要调整大小
                if (this.image.get(h).size() >= this.width) {
                    List<Integer> newList = new ArrayList<>(this.image.get(h).size() + 1);
                    newList.addAll(this.image.get(h));
                    this.image.set(h, newList);
                }
                if (this.energy.get(h).size() >= this.width) {
                    List<Integer> newList = new ArrayList<>(this.energy.get(h).size() + 1);
                    newList.addAll(this.energy.get(h));
                    this.energy.set(h, newList);
                }
                this.image.get(h).add(path[h], values[h]);
                this.energy.get(h).add(path[h], energyValues[h]);
            }
        });

        this.width += 1;
        if (this.update) {
            if (highlight) {
                this.updateImage(path, color);
            } else {
                this.updateImage();
            }
        }

        return true;
    }

    // 删除count个seam
    public int remove(int count, boolean highlight, int color) {
        if (count <= 0 || this.width <= 1)
            return 0;
        int removed = 0;
        for (int i = 0; i < count; i++) {
            if (this.remove(highlight, color)) {
                removed++;
            } else {
                break;
            }
        }
        return removed;
    }

    // 删除下一个seam
    public boolean remove(boolean highlight, int color) {
        if (this.width <= 1)
            return false;

        int[] path = new int[this.height];
        int[] values = new int[this.height];
        int[] energyValues = new int[this.height];

        int minIndex = Utils.argmin(this.map[0], this.width);
        path[0] = minIndex;
        values[0] = this.image.get(0).remove(minIndex);
        energyValues[0] = this.energy.get(0).remove(minIndex);

        for (int h = 1; h < this.height; h++) {
            int[] row = this.map[h];
            if (minIndex == 0) {
                minIndex = Utils.min(row[0], row[1]) == row[0] ? 0 : 1;
            } else if (minIndex == this.width - 1) {
                int minValue = Utils.min(row[this.width - 2], row[this.width - 1]);
                minIndex = row[this.width - 2] == minValue ? this.width - 2 : this.width - 1;
            } else {
                int minValue = Utils.min(row[minIndex - 1], row[minIndex], row[minIndex + 1]);
                if (row[minIndex - 1] == minValue)
                    minIndex = minIndex - 1;
                else if (row[minIndex + 1] == minValue)
                    minIndex = minIndex + 1;
            }
            path[h] = minIndex;
        }

        // 串行删除路径
        for (int h = 1; h < this.height; h++) {
            values[h] = this.image.get(h).remove(path[h]);
            energyValues[h] = this.energy.get(h).remove(path[h]);
        }

        this.width -= 1;
        if (this.update) {
            if (highlight) {
                this.updateImage(path, color);
            } else {
                this.updateImage();
            }
        }
        this.seams.push(path);
        this.values.push(values);
        this.energyValues.push(energyValues);
        return true;
    }

    // 更新当前图像
    public void updateImage(boolean highlight, int color) {
        if (highlight && !this.seams.isEmpty()) {
            this.updateImage(this.seams.peek(), color);
        } else {
            this.updateImage();
        }
    }

    // 更新当前图像以匹配图像的当前状态
    protected void updateImage() {
        for (int h = 0; h < this.height; h++) {
            for (int w = 0; w < this.width; w++) {
                this.data[h * this.maxWidth + w] = this.image.get(h).get(w);
            }
        }
        // 调试输出
        // System.out.println("updateImage: data = " +
        // java.util.Arrays.toString(this.data));
    }

    // 更新当前图像以匹配当前状态，突出显示路径
    protected void updateImage(int[] path, int color) {
        for (int h = 0; h < this.height; h++) {
            for (int w = 0; w < this.width; w++) {
                this.data[h * this.maxWidth + w] = this.image.get(h).get(w);
            }
            this.data[h * this.maxWidth + path[h]] = color; // 突出显示路径
        }
        // 调试输出
        // System.out.println("updateImage with path: data = " +
        // java.util.Arrays.toString(this.data));
    }
}
