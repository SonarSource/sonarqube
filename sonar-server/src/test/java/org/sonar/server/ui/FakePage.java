package org.sonar.server.ui;

import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.Page;

@NavigationSection(NavigationSection.RESOURCE)
public class FakePage implements Page {
  public String getId() {
    return "fake-page";
  }

  public String getTitle() {
    return "fake page";
  }
}
