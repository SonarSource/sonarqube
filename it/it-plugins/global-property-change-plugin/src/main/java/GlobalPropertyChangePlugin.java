import java.util.Arrays;
import java.util.List;
import org.sonar.api.SonarPlugin;

public class GlobalPropertyChangePlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(FakeGlobalPropertyChange.class);
  }
}
