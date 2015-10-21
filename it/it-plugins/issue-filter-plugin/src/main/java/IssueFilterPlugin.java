import java.util.Arrays;
import java.util.List;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;

@Properties(@Property(
  key = "enableIssueFilters",
  name = "Enable Issue Filters",
  defaultValue = "false",
  type = PropertyType.BOOLEAN))
public class IssueFilterPlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(IssueFilterBeforeLine5.class, ModuleIssueFilter.class);
  }
}
