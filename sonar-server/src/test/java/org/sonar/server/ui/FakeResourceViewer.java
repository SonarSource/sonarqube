package org.sonar.server.ui;

import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.Page;

@NavigationSection(NavigationSection.RESOURCE_TAB)
public class FakeResourceViewer implements Page {
  public String getId() {
    return "fake-resourceviewer";
  }

  public String getTitle() {
    return "fake-resourceviewer";
  }
}
