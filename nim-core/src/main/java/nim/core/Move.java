package nim.core;

/**
 * Move
 * @param heapIndex
 * @param count
 */
public record Move(int heapIndex, int count) {

    public Move {
        if (heapIndex < 0) {
            throw new IllegalArgumentException("heapIndex phải >= 0");
        }

        if (count < 1) {
            throw new IllegalArgumentException("Mỗi nước phải bốc ít nhất 1 vật phẩm");
        }
    }

    @Override
    public String toString() {
        return "Đống " + (heapIndex + 1) + ", bốc " + count + " vật phẩm";
    }
}