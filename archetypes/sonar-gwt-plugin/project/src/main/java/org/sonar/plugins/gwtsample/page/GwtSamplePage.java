package org.sonar.plugins.gwtsample.page;

import org.sonar.api.web.GwtPage;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.UserRole;

@NavigationSection(NavigationSection.RESOURCE)
@UserRole(UserRole.USER)
public class GwtSamplePage extends GwtPage {

  public String getGwtId() {
    return "org.sonar.plugins.gwtsample.page.SamplePage";
  }

  public String getTitle() {
    return "Sample";
  }
}
