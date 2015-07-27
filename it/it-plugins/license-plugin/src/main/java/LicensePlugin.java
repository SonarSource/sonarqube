import java.util.Collections;
import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;

@Properties({
    @Property(
        key = "untyped.license.secured",
        name = "Property without license type",
        category = CoreProperties.CATEGORY_GENERAL),
    @Property(
        key = "typed.license.secured",
        name = "Typed property",
        category = CoreProperties.CATEGORY_GENERAL,
        type = PropertyType.LICENSE)
})
public class LicensePlugin extends SonarPlugin {
  public List getExtensions() {
    return Collections.emptyList();
  }
}
