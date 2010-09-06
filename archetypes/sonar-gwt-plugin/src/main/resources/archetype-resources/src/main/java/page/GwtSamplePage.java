#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.page;

import org.sonar.api.web.GwtPage;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.UserRole;

@NavigationSection(NavigationSection.RESOURCE)
@UserRole(UserRole.USER)
public class GwtSamplePage extends GwtPage {

  public String getGwtId() {
    return "${package}.page.SamplePage";
  }

  public String getTitle() {
    return "Sample";
  }
}
