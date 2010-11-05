#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import org.sonar.api.web.*;

@UserRole(UserRole.USER)
@Description("Show how to use Ruby Widget API")
@WidgetProperties({
        @WidgetProperty(key="param1",
                        description="This is a mandatory parameter",
                        optional=false
        ),
        @WidgetProperty(key="max",
                        description="max threshold",
                        type=WidgetPropertyType.INTEGER,
                        defaultValue="80"
        ),
        @WidgetProperty(key="param2",
                        description="This is an optional parameter"
        ),
        @WidgetProperty(key="floatprop",
                        description="test description"
        )
})
public class SampleDashboardWidget extends AbstractRubyTemplate implements RubyRailsWidget {

  public String getId() {
    return "sample";
  }

  public String getTitle() {
    return "Sample";
  }

  @Override
  protected String getTemplatePath() {
    return "/sample_dashboard_widget.html.erb";
  }
}