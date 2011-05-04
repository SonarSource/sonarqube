package com.mycompany.sonar.gwt.page;

import org.sonar.api.web.GwtPage;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.UserRole;

@NavigationSection(NavigationSection.RESOURCE)
@UserRole(UserRole.USER)
public class SamplePage extends GwtPage {

  public String getGwtId() {
    return "com.mycompany.sonar.gwt.page.SamplePage";
  }

  public String getTitle() {
    return "Sample";
  }
}
