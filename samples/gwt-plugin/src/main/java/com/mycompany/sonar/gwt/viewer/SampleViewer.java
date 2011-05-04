package com.mycompany.sonar.gwt.viewer;

import org.sonar.api.web.GwtPage;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.UserRole;

@NavigationSection(NavigationSection.RESOURCE_TAB)
@UserRole(UserRole.USER)
public class SampleViewer extends GwtPage {
  public String getTitle() {
    return "Sample";
  }

  public String getGwtId() {
    return "com.mycompany.sonar.gwt.viewer.SampleViewer";
  }
}