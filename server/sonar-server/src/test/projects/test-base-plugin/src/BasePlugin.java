import org.sonar.api.SonarPlugin;

import java.util.Collections;
import java.util.List;

public class BasePlugin extends SonarPlugin {

  public List getExtensions() {
    return Collections.emptyList();
  }
}
