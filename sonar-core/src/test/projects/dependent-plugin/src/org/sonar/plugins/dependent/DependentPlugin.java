package org.sonar.plugins.dependent;

import org.sonar.api.Plugin;
import org.sonar.plugins.base.api.BaseApi;

public class DependentPlugin implements Plugin {

  public DependentPlugin() {
    // uses a class that is exported by base-plugin
    new BaseApi().doNothing();
  }

  public void define(Context context) {

  }
}
