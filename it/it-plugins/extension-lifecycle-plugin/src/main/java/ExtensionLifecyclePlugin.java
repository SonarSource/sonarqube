import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public class ExtensionLifecyclePlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(BatchService.class, ProjectService.class, ServerService.class);
  }
}
