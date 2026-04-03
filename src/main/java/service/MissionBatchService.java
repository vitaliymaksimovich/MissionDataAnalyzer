package service;

import model.Mission;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MissionBatchService {

    private final MissionService missionService;

    public MissionBatchService() {
        this.missionService = new MissionService();
    }

    public List<Mission> loadAll(File[] files) {
        List<Mission> result = new ArrayList<>();
        for (File file : files) {
            try {
                result.add(missionService.loadMission(file));
            } catch (IOException e) {
                System.err.println("[BATCH] Не удалось загрузить: " + file.getName() + " — " + e.getMessage());
            }
        }
        return result;
    }
}