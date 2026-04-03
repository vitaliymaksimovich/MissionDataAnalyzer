package service;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class AppLogger {

    public static final ByteArrayOutputStream BUFFER = new ByteArrayOutputStream();

    static {
        PrintStream original = System.out;
        PrintStream tee = new PrintStream(BUFFER) {
            @Override public void println(String x) { original.println(x); super.println(x); }
            @Override public void print(String x)   { original.print(x);   super.print(x); }
        };
        System.setOut(tee);
    }

    public static String getLogs() {
        return BUFFER.toString();
    }

    private AppLogger() {}
}
