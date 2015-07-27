import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.ServerExtension;

@Properties({
    @Property(key = "boolean", name = "Boolean", type=PropertyType.BOOLEAN),
    @Property(key = "integer", name = "Integer", type=PropertyType.INTEGER),
    @Property(key = "float", name = "Float", type=PropertyType.FLOAT),
    @Property(key = "password", name = "Password", type=PropertyType.PASSWORD, defaultValue = "sonar"),
    @Property(key = "text", name = "Text", type=PropertyType.TEXT),
    @Property(key = "metric", name = "Metric", type=PropertyType.METRIC),
    @Property(key = "single_select_list", name = "Single Select List", type=PropertyType.SINGLE_SELECT_LIST, options = {"de", "en", "nl"})
})
public class PropertyTypes implements ServerExtension {
}
