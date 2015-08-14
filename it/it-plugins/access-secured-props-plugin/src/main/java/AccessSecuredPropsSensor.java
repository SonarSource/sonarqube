import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

@Properties({
    @Property(
        key = "accessSecuredFromSensor",
        name = "Property to decide if sensor should access secured properties",
        defaultValue = "false")
})
public class AccessSecuredPropsSensor implements Sensor {

  private Settings settings;

  public AccessSecuredPropsSensor(Settings settings) {
    this.settings = settings;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void analyse(Project project, SensorContext sensorContext) {
    if ("true".equals(settings.getString("accessSecuredFromSensor"))) {
      settings.getString("foo.bar.secured");
    }
  }
}
