import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

@Properties({
    @Property(
        key = "crash",
        name = "Property to decide if it crash or not",
        defaultValue = "false")
})
public class CrashSensor implements Sensor {

  private Settings settings;

  public CrashSensor(Settings settings) {
    this.settings = settings;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void analyse(Project project, SensorContext sensorContext) {
    if ("true".equals(settings.getString("crash"))) {
      throw new RuntimeException("Crash!");
    }
  }
}
