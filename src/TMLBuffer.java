/**
 * Created by NitreExplosion on 2017/5/23.
 */
public class TMLBuffer {
    boolean busy;
    int operator;
    float[] value;
    int[][] rsIndex;
    int address;
    int pc;

    public TMLBuffer() {
        busy = false;
        value = new float[2];
        rsIndex = new int[2][2];
        rsIndex[0][0] = -1;
        rsIndex[1][0] = -1;

        address = -1;
        pc = -1;
    }
}
