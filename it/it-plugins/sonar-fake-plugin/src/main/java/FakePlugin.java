import org.sonar.api.SonarPlugin;

import java.util.Collections;
import java.util.List;

public final class FakePlugin extends SonarPlugin {

  public List getExtensions() {
    return Collections.emptyList();
  }

}
