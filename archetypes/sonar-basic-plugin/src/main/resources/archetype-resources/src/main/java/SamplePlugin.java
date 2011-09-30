#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import org.sonar.api.Properties;
import org.sonar.api.Property;

import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * This class is the entry point for all extensions
 */
@Properties({ 
    @Property(key = SamplePlugin.MY_PROPERTY,
    name = "Plugin Property",
    description = "A property for the plugin")})
public class SamplePlugin extends SonarPlugin {

  public static final String MY_PROPERTY = "sonar.sample.myproperty";

  // This is where you're going to declare all your Sonar extensions
  public List getExtensions() {
    return Arrays.asList(SampleMetrics.class, SampleSensor.class, SampleDashboardWidget.class);
  }

}