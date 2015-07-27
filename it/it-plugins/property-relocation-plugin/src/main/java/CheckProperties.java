import org.sonar.api.BatchExtension;
import org.sonar.api.config.Settings;

public class CheckProperties implements BatchExtension {
  private Settings settings;

  public CheckProperties(Settings settings) {
    this.settings = settings;
  }

  public void start() {
    if (settings.getBoolean("sonar.newKey") != true) {
      throw new IllegalStateException("Property not found: sonar.newKey");
    }
  }
}
