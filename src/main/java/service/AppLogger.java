package service;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AppLogger {

    public static final ByteArrayOutputStream BUFFER = new ByteArrayOutputStream();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

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

    public static void log(String message) {
        System.out.println("[LOG " + now() + "] " + message);
    }

    public static void warn(String message) {
        System.out.println("[WARN " + now() + "] " + message);
    }

    public static void error(String message) {
        System.out.println("[ERROR " + now() + "] " + message);
    }

    public static void export(String type, String path) {
        System.out.println("[EXPORT " + now() + "] " + type + " → " + path);
    }

    private static String now() {
        return LocalDateTime.now().format(FMT);
    }

    private AppLogger() {}
}