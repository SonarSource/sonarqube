package org.sonar.plugins.sample;

import org.sonar.api.Extension;
import org.sonar.api.Plugin;

import java.util.Collections;
import java.util.List;

public class SamplePlugin implements Plugin {
  public String getKey() {
    return "sample";
  }

  public String getName() {
    return "My first Sonar plugin";
  }

  public String getDescription() {
    return "You shouldn't expect too much from this plugin.";
  }

  public List<Class<? extends Extension>> getExtensions() {
    return Collections.emptyList();
  }

  @Override
  public String toString() {
    return getKey();
  }
}
