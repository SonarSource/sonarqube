import org.sonar.api.SonarPlugin;

import java.util.Collections;
import java.util.List;

public class Test2Plugin extends SonarPlugin {

  public List getExtensions() {
    return Collections.emptyList();
  }
}
