import java.util.Arrays;
import java.util.List;
import org.sonar.api.SonarPlugin;

public class SettingsPlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(ServerExtensionWithProperties.class, PropertyTypes.class);
  }
}
