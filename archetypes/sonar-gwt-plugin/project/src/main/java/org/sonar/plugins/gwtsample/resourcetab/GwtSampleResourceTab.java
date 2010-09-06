package org.sonar.plugins.gwtsample.resourcetab;

import org.sonar.api.web.GwtPage;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.UserRole;

@NavigationSection(NavigationSection.RESOURCE_TAB)
@UserRole(UserRole.USER)
public class GwtSampleResourceTab extends GwtPage {
  public String getTitle() {
    return "Sample";
  }

  public String getGwtId() {
    return "org.sonar.plugins.gwtsample.resourcetab.SampleResourceTab";
  }
}