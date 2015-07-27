import java.util.Arrays;
import java.util.List;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.SonarPlugin;

@Properties({
  @Property(key = "sonar.newKey", deprecatedKey = "sonar.deprecatedKey", name = "New Key", category = "general")
})
public class PropertyRelocationPlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(CheckProperties.class);
  }
}
