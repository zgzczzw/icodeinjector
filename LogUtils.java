package com.icodeinjector;

import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Stack;

public class LogUtils {
    public static final String TAG = "codeInjector";
    public static final String VER = "v1.1";
    public static final int sDefaultMethodPosInThread = 5;
    public static int sCurrentMethodPosInThread = 0;
    public static String[] sLogWhiteListPackage = {"com.antfortune", "com.tencent"};
    public static String[] sLogBlackListPackage = {"com.tencent.gmtrace"};
    public static long fileModifyTime = System.currentTimeMillis();
    public static String logSwitch = "1";

    public static final String sConfigFilePath = "/data/local/tmp/log.txt";
    public static File file = new File(sConfigFilePath);
    public static String cmdLineStr = null;

    public static Stack<Long> sMethodStartTimeRecordStack = new Stack<Long>();

    static {
        Log.i(TAG, "LogUtils Ver. " + VER);
        cmdLineStr = getCmdArgs();
        parseCmdLineArgs(cmdLineStr);
        fileModifyTime = file.lastModified();
    }

    public static void preLog() {
        if (!isShowLog()) {
            return;
        }
        StackTraceElement element = getCurrentMethodName("preLog");
        if (shouldPrintLog(element.toString())) {
            Log.i(TAG, element + " IN");
            sMethodStartTimeRecordStack.push(SystemClock.elapsedRealtime());
        }
    }

    public static void postLog() {
        if (!isShowLog()) {
            return;
        }
        StackTraceElement element = getCurrentMethodName("postLog");
        if (shouldPrintLog(element.toString())) {
            long methodTime = getMethodTime();
            Log.i(TAG, element + " OUT cost:" + methodTime + "ms");
        }
    }

    public static void postLog(int outPos) {
        if (!isShowLog()) {
            return;
        }
        StackTraceElement element = getCurrentMethodName("postLog");
        if (shouldPrintLog(element.toString())) {
            long methodTime = getMethodTime();
            Log.i(TAG, element + " OUT #" + outPos + " cost:" + methodTime + "ms");
        }
    }

    public static long getMethodTime() {
        if (sMethodStartTimeRecordStack.size() > 0) {
            long methodStartTime = sMethodStartTimeRecordStack.pop();
            return SystemClock.elapsedRealtime() - methodStartTime;
        }
        return 0;
    }

    public static boolean shouldPrintLog(String threadStackElement) {
        if (!isShowLog()) {
            return false;
        }
        if (sLogWhiteListPackage.length > 0) {
            for (String blackListPackage : sLogBlackListPackage) {
                if (threadStackElement.contains(blackListPackage)) {
                    return false;
                }
            }
            for (String whileListPackage : sLogWhiteListPackage) {
                if (threadStackElement.contains(whileListPackage)) {
                    return true;
                }
            }
            return false;
        } else {
            for (String blackListPackage : sLogBlackListPackage) {
                if (threadStackElement.contains(blackListPackage)) {
                    return false;
                }
            }
            return true;
        }
    }

    public static StackTraceElement getCurrentMethodName(String preMethodName) {
        boolean needFindMethodPos = true;
        boolean foundMethodPos = false;
        StackTraceElement[] stackElements = Thread.currentThread().getStackTrace();
        if (sCurrentMethodPosInThread != 0) {
            if (stackElements[sCurrentMethodPosInThread - 2].toString().contains(preMethodName)) {
                needFindMethodPos = false;
            }
        }
        if (needFindMethodPos) {
            for (int i = 0; i < stackElements.length; i++) {
                StackTraceElement element = stackElements[i];
                if (element.toString().contains(preMethodName)) {
                    sCurrentMethodPosInThread = (i + 2);
                    foundMethodPos = true;
                    break;
                }
            }
            if (!foundMethodPos) {
                sCurrentMethodPosInThread = sDefaultMethodPosInThread;
            }
        }

        if (stackElements.length >= sCurrentMethodPosInThread) {
            return stackElements[sCurrentMethodPosInThread - 1];
        } else {
            return stackElements[stackElements.length - 1];
        }
    }

    /**
     * @return
     */
    private static boolean isShowLog() {
        if (fileModifyTime != file.lastModified()) {
            fileModifyTime = file.lastModified();
            cmdLineStr = getCmdArgs();
            parseCmdLineArgs(cmdLineStr);
        }
        if ("1".equals(logSwitch)) {
            return true;
        }
        return false;
    }

    /**
     * 获取命令行参数信息
     *
     * @return
     */
    public static String getCmdArgs() {
        File file = new File(sConfigFilePath);
        BufferedReader bf = null;
        try {
            bf = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = bf.readLine()) != null) {
                if (line != null) {
                    return line;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bf != null)
                    bf.close();
            } catch (Exception e) {

            }
        }
        return null;
    }

    /**
     * 解析命令行参数信息
     * -s 1
     *
     * @param cmdLine
     */
    private static void parseCmdLineArgs(String cmdLine) {
        try {
            String[] strAry = cmdLine.split(" ");
            for (int i = 0; i < strAry.length; i += 2) {
                switch (strAry[i]) {
                    case "-s":
                        logSwitch = strAry[i + 1];
                        break;
                }
            }
        } catch (Exception e) {
        }
    }
}
