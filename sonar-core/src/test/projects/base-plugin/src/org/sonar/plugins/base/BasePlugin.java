package org.sonar.plugins.base;

import java.util.Collections;
import java.util.List;
import org.sonar.api.SonarPlugin;

public class BasePlugin extends SonarPlugin {

  public List getExtensions() {
    return Collections.emptyList();
  }
}
