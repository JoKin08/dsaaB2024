import java.util.List;

// 使用反向能量实现seam carve
public class SeamCarverBackward extends SeamCarverBase implements SeamCarver {

    public SeamCarverBackward(int[][] image) {
        super(image);
        this.energy = Utils.sobel(image);
        this.energyMap();
    }

    public boolean add(boolean highlight, int color) {
        boolean valid = super.add(highlight, color);
        if (valid)
            this.energyMap();
        return valid;
    }

    public boolean remove(boolean highlight, int color) {
        boolean valid = super.remove(highlight, color);
        if (valid)
            this.energyMap();
        return valid;
    }

    // 从梯度图像创建能量图
    private void energyMap() {
        for (int w = 0; w < this.width; w++) {
            this.map[this.height - 1][w] = this.energy.get(this.height - 1).get(w);
        }
        // 从底部向上计算能量
        for (int h = this.height - 2; h >= 0; h--) {
            List<Integer> row = this.energy.get(h);
            this.map[h][0] = row.get(0) + Utils.min(this.map[h + 1][0], this.map[h + 1][1]);
            int w;
            for (w = 1; w < this.width - 1; w++) {
                this.map[h][w] = row.get(w) +
                        Utils.min(this.map[h + 1][w - 1], this.map[h + 1][w], this.map[h + 1][w + 1]);
            }
            this.map[h][w] = row.get(w) + Utils.min(this.map[h + 1][w - 1], this.map[h + 1][w]);
        }
    }
}
