import org.sonar.api.web.AbstractRubyTemplate;
import org.sonar.api.web.RubyRailsWidget;
import org.sonar.api.web.WidgetScope;

import static org.sonar.api.web.WidgetScope.GLOBAL;

@WidgetScope(GLOBAL)
public class WidgetDisplayingProperties extends AbstractRubyTemplate implements RubyRailsWidget {

  public String getId() {
    return "widget-displaying-properties";
  }

  public String getTitle() {
    return "Widget Displaying Properties";
  }

  @Override
  protected String getTemplatePath() {
    return "/widgets/widget-displaying-properties.html.erb";
  }
}
