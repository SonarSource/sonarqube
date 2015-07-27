import java.util.Arrays;
import java.util.List;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.SonarPlugin;

@Properties({
  @Property(key = "some-property", name = "Some Property", defaultValue = "aDefaultValue", global = true, project = false)
})
public class ServerPlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(WidgetDisplayingProperties.class, TempFolderExtension.class);
  }
}
