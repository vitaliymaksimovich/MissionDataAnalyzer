import model.Mission;
import service.MissionReportFormatter;
import service.MissionService;

import java.io.File;

public class Main {

    public static void main(String[] args) {
        try {

            MissionService missionService = new MissionService();
            MissionReportFormatter formatter = new MissionReportFormatter();

            File file = new File("C:\\Users\\vital\\OneDrive\\Рабочий стол\\Mission B.json");

            Mission mission = missionService.loadMission(file);

            String report = formatter.format(mission);

            System.out.println(report);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
