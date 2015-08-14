import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.task.TaskExtension;
import org.sonar.api.resources.Project;

@Properties({
    @Property(
        key = "accessSecuredFromTask",
        name = "Property to decide if task extension should access secured properties",
        defaultValue = "false")
})
public class AccessSecuredPropsTaskExtension implements TaskExtension {

  private Settings settings;

  public AccessSecuredPropsTaskExtension(Settings settings) {
    this.settings = settings;
  }

  public void start() {
    if ("true".equals(settings.getString("accessSecuredFromTask"))) {
      settings.getString("foo.bar.secured");
    }
  }
}
