import org.sonar.api.SonarPlugin;

import java.util.Collections;
import java.util.List;

public class RequirePlugin extends SonarPlugin {

  public RequirePlugin() {
    // call a class that is in the api published by the base plugin
    new org.sonar.plugins.testbase.api.BaseApi().doNothing();
  }

  public List getExtensions() {
    return Collections.emptyList();
  }
}
