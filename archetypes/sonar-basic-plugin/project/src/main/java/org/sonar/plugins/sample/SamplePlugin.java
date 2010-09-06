package org.sonar.plugins.sample;

import org.sonar.api.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is the entry point for all extensions
 */
public class SamplePlugin implements Plugin {

  /**
   * @deprecated this is not used anymore
   */
  public String getKey() {
    return "sample";
  }

  /**
   * @deprecated this is not used anymore
   */
  public String getName() {
    return "My Sonar plugin";
  }

  /**
   * @deprecated this is not used anymore
   */
  public String getDescription() {
    return "You shouldn't expect too much from this plugin except displaying the Hello World message.";
  }

  // This is where you're going to declare all your Sonar extensions
  public List getExtensions() {
    return Arrays.asList(SampleMetrics.class, SampleSensor.class, SampleDashboardWidget.class);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}