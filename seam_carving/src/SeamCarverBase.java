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

    // 构造函数接受 2D 图像数组
    public SeamCarverBase(int[][] image) {
        this.height = image.length;
        this.width = image[0].length;
        this.update = true;
        this.seams = new Stack<>();
        this.values = new Stack<>();
        this.energyValues = new Stack<>();
        this.image = new ArrayList<>(this.height);
        this.data = new int[this.height * this.width];
        this.map = new int[this.height][this.width];

        for (int h = 0; h < this.height; h++) {
            this.image.add(new ArrayList<>(this.width));
        }

        Utils.parallel((cpu, cpus) -> {
            for (int h = cpu; h < this.height; h += cpus) {
                for (int w = 0; w < this.width; w++) {
                    this.image.get(h).add(image[h][w]);
                    this.data[h * this.width + w] = image[h][w];
                }
            }
        });
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
        if (this.seams.isEmpty() || count <= 0) return 0;
        this.update = false;
        int index = 1;
        while (index++ < count && this.seams.size() > 1) {
            this.add(highlight, color);
        }
        this.update = true;
        this.add(highlight, color);
        return index - 1;
    }

    // 将最近删除的seam添加回图像
    public boolean add(boolean highlight, int color) {
        if (this.seams.isEmpty()) return false;

        int[] path = this.seams.pop();
        int[] values = this.values.pop();
        int[] energy = this.energyValues.pop();

        Utils.parallel((cpu, cpus) -> {
            for (int i = cpu; i < path.length; i += cpus) {
                this.image.get(i).add(path[i], values[i]);
                this.energy.get(i).add(path[i], energy[i]);
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
        if (this.width == 2 || count <= 0) return 0;
        this.update = false;
        int index = 1;
        while (index++ < count && this.width > 3) {
            this.remove(highlight, color);
        }
        this.update = true;
        this.remove(highlight, color);
        return index - 1;
    }

    // 删除下一个seam
    public boolean remove(boolean highlight, int color) {
        if (this.width == 2) return false;

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
                if (row[minIndex - 1] == minValue) minIndex = minIndex - 1;
                else if (row[minIndex + 1] == minValue) minIndex = minIndex + 1;
            }
            path[h] = minIndex;
        }

        Utils.parallel((cpu, cpus) -> {
            for (int h = 1 + cpu; h < this.height; h += cpus) {
                values[h] = this.image.get(h).remove(path[h]);
                energyValues[h] = this.energy.get(h).remove(path[h]);
            }
        });

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
        Utils.parallel((cpu, cpus) -> {
            for (int h = cpu; h < this.height; h += cpus) {
                for (int w = 0; w < this.width; w++) {
                    this.data[h * this.width + w] = this.image.get(h).get(w);
                }
            }
        });
    }

    // 更新当前图像以匹配图像的当前状态，同时显示highlight
    protected void updateImage(int[] path, int color) {
        Utils.parallel((cpu, cpus) -> {
            for (int h = cpu; h < this.height; h += cpus) {
                for (int w = 0; w < this.width; w++) {
                    this.data[h * this.width + w] = this.image.get(h).get(w);
                }
                int pathIndex = path[h];
                for (int i = pathIndex - 1; i <= pathIndex + 1; i++) {
                    if (i < 0 || i >= this.width) continue;
                    this.data[h * this.width + i] = color;
                }
            }
        });
    }
}
