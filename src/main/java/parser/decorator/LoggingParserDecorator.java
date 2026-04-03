package parser.decorator;

import model.Mission;
import parser.MissionParser;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggingParserDecorator extends MissionParser {

    private final MissionParser delegate;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public LoggingParserDecorator(MissionParser delegate) {
        this.delegate = delegate;
    }

    @Override
    public Mission parse(File file) throws IOException {
        log("Начало разбора: " + file.getName());
        long start = System.currentTimeMillis();
        try {
            Mission mission = delegate.parse(file);
            long ms = System.currentTimeMillis() - start;
            log("Готово: " + file.getName() + " [" + ms + " мс] → миссия " + mission.getMissionId());
            return mission;
        } catch (Exception e) {
            log("ОШИБКА: " + file.getName() + " — " + e.getMessage());
            throw e;
        }
    }

    private void log(String message) {
        System.out.println("[LOG " + LocalDateTime.now().format(FMT) + "] " + message);
    }
}