package org.sonar.plugins.sample;

import org.sonar.api.Extension;
import org.sonar.api.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is the container for all others extensions
 */
public class SamplePlugin implements Plugin {

  // The key which uniquely identifies your plugin among all others Sonar plugins

  public String getKey() {
    return "sample";
  }

  public String getName() {
    return "My first Sonar plugin";
  }

  // This description will be displayed in the Configuration > Settings web page

  public String getDescription() {
    return "You shouldn't expect too much from this plugin except displaying the Hello World message.";
  }

  // This is where you're going to declare all your Sonar extensions

  public List<Class<? extends Extension>> getExtensions() {
    List<Class<? extends Extension>> list = new ArrayList<Class<? extends Extension>>();

    list.add(SampleMetrics.class);
    list.add(SampleSensor.class);

    return list;
  }

  @Override
  public String toString() {
    return getKey();
  }
}
