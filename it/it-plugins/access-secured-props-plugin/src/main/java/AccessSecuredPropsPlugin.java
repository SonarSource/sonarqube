import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public class AccessSecuredPropsPlugin extends SonarPlugin {

  public List getExtensions() {
    return Arrays.asList(AccessSecuredPropsSensor.class, AccessSecuredPropsTaskExtension.class);
  }

}
