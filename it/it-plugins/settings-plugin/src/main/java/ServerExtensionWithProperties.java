import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.ServerExtension;
import org.sonar.api.config.Settings;

@Properties({
    @Property(key = "settings.extension.hidden", name = "Hidden Property", description = "Hidden Property defined on extension but not plugin", global = false, project = false, module = false, defaultValue = "teahupoo"),
    @Property(key = "settings.extension.global", name = "Global Property", global = true, project = false, module = false)
})
public final class ServerExtensionWithProperties implements ServerExtension {

  private Settings settings;

  public ServerExtensionWithProperties(Settings settings) {
    this.settings = settings;
  }

  public void start() {
    System.out.println("Test that the default value of properties are automatically injected by the component Settings");
    if (!"teahupoo".equals(settings.getString("settings.extension.hidden"))) {
      throw new IllegalStateException("The property settings.extension.hidden is not registered");
    }
  }
}
