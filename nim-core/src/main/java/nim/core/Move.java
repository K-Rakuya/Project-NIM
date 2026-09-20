package nim.core;

public class Move {
    private final int heapIndex;
    private final int count;

    public Move(int heapIndex, int count) {
        if (heapIndex < 0) {
            throw new IllegalArgumentException("heapIndex phải >= 0");
        }
        if (count < 1) {
            throw new IllegalArgumentException("Mỗi bước đi phải bốc ít nhất 1 vật phẩm");
        }
        this.heapIndex = heapIndex;
        this.count = count;
    }

    public int getHeapIndex() {
        return heapIndex;
    }

    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "Đống " + (heapIndex + 1) + ", bốc " + count + " vật phẩm";
    }
}