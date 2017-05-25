import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by NitreExplosion on 2017/5/23.
 */

public class Pipeline {
    int[] cpuRegisters;
    int[] fpRegisters;

    int[][] fpRegistersStatus;

    ArrayList<String> cmdList;

    TMLBuffer[][] buffers;
    int pc;

    boolean isRunning;
    int runTimes;

    float[] memory;

    int[] runningRS;
    int[] restTime;

    public Pipeline() {
        cpuRegisters = new int[8];
        fpRegisters = new int[31];
        fpRegistersStatus = new int[31][2];

        cmdList = new ArrayList<>();
        pc = 0;

        buffers = new TMLBuffer[4][3];
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 3; j++)
                buffers[i][j] = new TMLBuffer();

        isRunning = false;
        runTimes = 0;

        memory = new float[4096];

        runningRS = new int[4];
        restTime = new int[4];
        for (int i = 0; i < 4; i++) {
            runningRS[i] = -1;
            restTime[i] = -1;
        }
    }

    public int run() {
        if (isRunning)
            return -1;
        if (cmdList.size() == 0)
            return -1;
        isRunning = true;
        pc = 0;

        if (runTimes > 0) {
            cpuRegisters = new int[8];
            fpRegisters = new int[31];
            fpRegistersStatus = new int[31][2];
            for (int i = 0; i < 32; i++) {
                fpRegistersStatus[i][0] = -1;
                fpRegisters[i] = 0;
            }

            cmdList = new ArrayList<>();
            pc = 0;

            buffers = new TMLBuffer[4][3];
            for (int i = 0; i < 4; i++)
                for (int j = 0; j < 3; j++)
                    buffers[i][j] = new TMLBuffer();

            memory = new float[4096];
        }
        runTimes++;
        return 0;
    }

    public int nextStep() {
        if (!isRunning)
            return -1;
        if (cmdList.size() == 0)
            return -1;
        whichRSRun();
        if (exe() != 0) return -1;
        if (writeResult() != 0) return -1;
        if (issue() != 0) return -1;
        return 0;
    }

    public int nextStep(int step) {
        if (!isRunning)
            return -1;
        if (cmdList.size() == 0)
            return -1;
        for (int i = 0; i < step; i++) {
            whichRSRun();
            if (exe() != 0) return -1;
            if (writeResult() != 0) return -1;
            if (issue() != 0) return -1;
        }
        return 0;
    }

    private int issue() {
        if (pc < cmdList.size()) {
            System.out.println(pc);
            String cmd = cmdList.get(pc++);
            String[] opList = cmd.split(" |,");

            opList[0] = opList[0].toUpperCase();

            Pattern p = Pattern.compile("^F([0-9]+)$");
            int bufferNum;
            int addr;
            int[] registerNum = new int[3];
            switch (opList[0]) {
                case "ADDD":
                case "SUBD": {
                    System.out.println("加减法指令");
                    bufferNum = 0;
                    break;
                }
                case "MULD":
                case "DIVD": {
                    System.out.println("乘除法指令");
                    bufferNum = 1;
                    break;
                }
                case "LD": {
                    System.out.println("读取指令");
                    bufferNum = 2;
                    break;
                }
                case "ST": {
                    System.out.println("写入指令");
                    bufferNum = 3;
                    break;
                }
                default:
                    System.out.println(String.format("第%d条代码格式有误,操作数不存在", pc + 1));
                    return -1;
            }
            if (bufferNum <= 1) {
                if (opList.length != 4)
                    return -1;
                for (int i = 0; i < 3; i++) {
                    Matcher m = p.matcher(opList[i + 1]);
                    if (m.find()) {
                        registerNum[i] = Integer.parseInt(m.group(1));
                        if (registerNum[i] >= 31) {
                            System.out.println(String.format("第%d条代码格式有误,寄存器编号越界", pc + 1));
                            return -1;
                        }
                    } else return -1;
                }
                boolean flag = false;
                for (int i = 0; i < 3; i++)
                    if (!buffers[bufferNum][i].busy) {
                        buffers[bufferNum][i].busy = true;
                        if (fpRegistersStatus[registerNum[1]][0] >= 0) {
                            buffers[bufferNum][i].rsIndex[0][0] = fpRegistersStatus[registerNum[1]][0];
                            buffers[bufferNum][i].rsIndex[0][1] = fpRegistersStatus[registerNum[1]][1];
                        } else buffers[bufferNum][i].value[0] = fpRegisters[registerNum[1]];
                        if (fpRegistersStatus[registerNum[2]][0] >= 0) {
                            buffers[bufferNum][i].rsIndex[1][0] = fpRegistersStatus[registerNum[2]][0];
                            buffers[bufferNum][i].rsIndex[1][1] = fpRegistersStatus[registerNum[2]][1];
                        } else buffers[bufferNum][i].value[1] = fpRegisters[registerNum[2]];

                        fpRegistersStatus[registerNum[0]][0] = 0;
                        fpRegistersStatus[registerNum[0]][1] = i;

                        if (bufferNum == 0)
                            buffers[0][i].operator = opList[0].equals("ADDD") ? 0 : 1;
                        else
                            buffers[1][i].operator = opList[0].equals("MULD") ? 0 : 1;
                        flag = true;
                        break;
                    }
                if (!flag)
                    pc--;
            } else {
                if (opList.length != 3)
                    return -1;
                Matcher m = p.matcher(opList[1]);
                if (m.find()) {
                    registerNum[0] = Integer.parseInt(m.group(1));
                    if (registerNum[0] >= 31) {
                        System.out.println(String.format("第%d条代码格式有误,寄存器编号越界", pc + 1));
                        return -1;
                    }
                } else return -1;

                Pattern addrP = Pattern.compile("^([0-9]+)\\(R([0-9]+)\\)$");
                Matcher addrM = addrP.matcher(opList[2]);
                if (addrM.find()) {
                    int cpuRNum = Integer.parseInt(addrM.group(2));
                    if (cpuRNum > 8) {
                        System.out.println(String.format("第%d条代码格式有误,CPU寄存器编号越界", pc + 1));
                        return -1;
                    }
                    addr = Integer.parseInt(addrM.group(1)) + cpuRegisters[cpuRNum];
                } else {
                    addrP = Pattern.compile("^([0-9]+)$");
                    addrM = addrP.matcher(opList[2]);
                    if (addrM.find()) {
                        addr = Integer.parseInt(addrM.group(1));
                    } else {
                        System.out.println(String.format("第%d条代码语法有误", pc + 1));
                        return -1;
                    }
                }

                if (addr < 0 || addr >= 4096) {
                    System.out.println(String.format("第%d条代码格式有误,内存地址编号越界", pc + 1));
                    return -1;
                }

                if (addr % 4 != 0) {
                    System.out.println(String.format("第%d条代码执行出错,内存地址未对齐", pc + 1));
                    return -1;
                }

                boolean flag = false;
                for (int i = 0; i < 3; i++)
                    if (!buffers[bufferNum][i].busy) {
                        buffers[bufferNum][i].busy = true;
                        buffers[bufferNum][i].address = addr;

                        if (bufferNum == 3)
                            if (fpRegistersStatus[registerNum[0]][0] >= 0) {
                                buffers[bufferNum][i].rsIndex[0][0] = fpRegistersStatus[registerNum[0]][0];
                                buffers[bufferNum][i].rsIndex[0][1] = fpRegistersStatus[registerNum[0]][1];
                            }

                        if (bufferNum == 2) {
                            fpRegistersStatus[registerNum[0]][0] = 0;
                            fpRegistersStatus[registerNum[0]][1] = i;
                        }
                        flag = true;
                        break;
                    }
                if (!flag)
                    pc--;
            }
        }

        return 0;
    }

    private int whichRSRun() {
        for (int i = 0; i < 4; i++) {
            if (runningRS[i] == -1) {
                for (int j = 0; j < 3; j++) {
                    if (buffers[i][j].busy)
                        if (buffers[i][j].rsIndex[0][0] == -1 && buffers[i][j].rsIndex[1][0] == -1) {
                            runningRS[i] = j;
                            switch (i) {
                                case 0: {
                                    restTime[i] = 2;
                                    break;
                                }
                                case 1: {
                                    if (buffers[i][j].operator == 0)
                                        restTime[i] = 10;
                                    else restTime[i] = 40;
                                    break;
                                }
                                case 2:
                                case 3: {
                                    restTime[i] = 2;
                                    break;
                                }
                            }
                        }
                }
            }
        }
        return 0;
    }

    private int exe() {
        for (int i = 0; i < 4; i++) {
            if (runningRS[i] != -1 && restTime[i] >= 0)
                restTime[i]--;
        }
        return 0;
    }

    private int writeResult() {
        for (int i = 0; i < 4; i++) {
            if (runningRS[i] != -1 && restTime[i] == -1) {
                float result = 0;
                switch (i) {
                    case 0: {
                        result = buffers[i][runningRS[i]].value[0];
                        if (buffers[i][runningRS[i]].operator == 0)
                            result += buffers[i][runningRS[i]].value[1];
                        else result -= buffers[i][runningRS[i]].value[1];
                        break;
                    }
                    case 1: {
                        result = buffers[i][runningRS[i]].value[0];
                        if (buffers[i][runningRS[i]].operator == 0)
                            result *= buffers[i][runningRS[i]].value[1];
                        else result /= buffers[i][runningRS[i]].value[1];
                        break;
                    }
                    case 2: {
                        result = memory[buffers[i][runningRS[i]].address];
                        break;
                    }
                    case 3: {
                        memory[buffers[i][runningRS[i]].address] = buffers[i][runningRS[i]].value[0];
                        break;
                    }
                }
                if (i == 3) {
                    for (int j = 0; j < 31; j++)
                        if (fpRegistersStatus[j][0] == i && fpRegistersStatus[j][1] == runningRS[i]) {
                            fpRegistersStatus[j][0] = fpRegistersStatus[j][1] = -1;
                            break;
                        }
                    buffers[i][runningRS[i]] = new TMLBuffer();
                    runningRS[i] = -1;
                    break;
                }
                for (int j = 0; j < 4; j++)
                    for (int k = 0; k < 3; k++)
                        if (buffers[j][k].busy) {
                            if (buffers[j][k].rsIndex[0][0] == i && buffers[j][k].rsIndex[0][1] == runningRS[i]) {
                                buffers[j][k].rsIndex[0][0] = buffers[j][k].rsIndex[0][1] = -1;
                                buffers[j][k].value[0] = result;
                            }
                            if (buffers[j][k].rsIndex[1][0] == i && buffers[j][k].rsIndex[1][1] == runningRS[i]) {
                                buffers[j][k].rsIndex[1][0] = buffers[j][k].rsIndex[1][1] = -1;
                                buffers[j][k].value[1] = result;
                            }
                        }
                for (int j = 0; j < 31; j++)
                    if (fpRegistersStatus[j][0] == i && fpRegistersStatus[j][1] == runningRS[i]) {
                        fpRegistersStatus[j][0] = fpRegistersStatus[j][1] = -1;
                        break;
                    }
                buffers[i][runningRS[i]] = new TMLBuffer();
                runningRS[i] = -1;
                break;
            }
        }
        return 0;
    }

    public int addCmd(String cmd) {
        if (isRunning)
            return -1;
        cmdList.add(cmd);
        return 0;
//        System.out.print(cmdList);
    }

    public enum BufferName {
        LOAD, STORE, ADD, MULT;
    }
}
