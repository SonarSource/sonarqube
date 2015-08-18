/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.ui;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.web.DefaultTab;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.RequiredMeasures;
import org.sonar.api.web.UserRole;
import org.sonar.api.web.View;
import org.sonar.api.web.Widget;
import org.sonar.api.web.WidgetProperties;
import org.sonar.api.web.WidgetProperty;
import org.sonar.api.web.WidgetPropertyType;
import org.sonar.api.web.WidgetScope;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newProjectDto;

public class ViewProxyTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void compareTo() {
    assertThat(new ViewProxy<>(new FakeView("aaa"), userSession).compareTo(new ViewProxy<>(new FakeView("bbb"), userSession))).isLessThan(0);
    assertThat(new ViewProxy<>(new FakeView("aaa"), userSession).compareTo(new ViewProxy<>(new FakeView("aaa"), userSession))).isZero();
    assertThat(new ViewProxy<>(new FakeView("bbb"), userSession).compareTo(new ViewProxy<>(new FakeView("aaa"), userSession))).isGreaterThan(0);
  }

  @Test
  public void doLoadMetadata() {

    @UserRole(UserRole.ADMIN)
    @NavigationSection(NavigationSection.RESOURCE)
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }

    View view = new MyView();
    ViewProxy<?> proxy = new ViewProxy<>(view, userSession);

    assertThat(proxy.getTarget()).isEqualTo(view);
    assertThat(proxy.getSections()).isEqualTo(new String[] {NavigationSection.RESOURCE});
    assertThat(proxy.getUserRoles()).isEqualTo(new String[] {UserRole.ADMIN});
  }

  @Test
  public void doLoadDefaultMetadata() {

    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }
    View view = new MyView();
    ViewProxy<?> proxy = new ViewProxy<>(view, userSession);

    assertThat(proxy.getTarget()).isEqualTo(view);
    assertThat(proxy.getSections()).isEqualTo(new String[] {NavigationSection.HOME});
    assertThat(proxy.getUserRoles()).isEmpty();
  }

  @Test
  public void isDefaultTab() {
    @DefaultTab
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }

    ViewProxy<?> proxy = new ViewProxy<>(new MyView(), userSession);

    assertThat(proxy.isDefaultTab()).isTrue();
    assertThat(proxy.getDefaultTabForMetrics()).isEmpty();
  }

  @Test
  public void isNotDefaultTab() {
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }
    ViewProxy<?> proxy = new ViewProxy<>(new MyView(), userSession);

    assertThat(proxy.isDefaultTab()).isFalse();
    assertThat(proxy.getDefaultTabForMetrics()).isEmpty();
  }

  @Test
  public void isDefaultTabForMetrics() {
    @DefaultTab(metrics = {"ncloc", "coverage"})
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }
    ViewProxy<?> proxy = new ViewProxy<>(new MyView(), userSession);

    assertThat(proxy.isDefaultTab()).isFalse();
    assertThat(proxy.getDefaultTabForMetrics()).isEqualTo(new String[] {"ncloc", "coverage"});
  }

  @Test
  public void widget_should_be_editable() {
    ViewProxy<Widget> proxy = new ViewProxy<Widget>(new EditableWidget(), userSession);

    assertThat(proxy.isEditable()).isTrue();
    assertThat(proxy.getWidgetProperties()).hasSize(3);
  }

  @Test
  public void load_widget_properties_in_the_same_order_than_annotations() {
    ViewProxy<Widget> proxy = new ViewProxy<Widget>(new EditableWidget(), userSession);

    List<WidgetProperty> widgetProperties = Lists.newArrayList(proxy.getWidgetProperties());
    assertThat(widgetProperties).hasSize(3);
    assertThat(widgetProperties.get(0).key()).isEqualTo("first_prop");
    assertThat(widgetProperties.get(1).key()).isEqualTo("second_prop");
    assertThat(widgetProperties.get(2).key()).isEqualTo("third_prop");
  }

  @Test
  public void widget_should_have_text_property() {
    ViewProxy<Widget> proxy = new ViewProxy<Widget>(new TextWidget(), userSession);

    assertThat(getOnlyElement(proxy.getWidgetProperties()).type()).isEqualTo(WidgetPropertyType.TEXT);
  }

  @Test
  public void widget_should_not_be_global_by_default() {
    ViewProxy<Widget> proxy = new ViewProxy<Widget>(new EditableWidget(), userSession);

    assertThat(proxy.isGlobal()).isFalse();
  }

  @Test
  public void widget_should_be_global() {
    ViewProxy<Widget> proxy = new ViewProxy<Widget>(new GlobalWidget(), userSession);

    assertThat(proxy.isGlobal()).isTrue();
  }

  @Test
  public void should_fail_to_load_widget_with_invalid_scope() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("INVALID");
    exception.expectMessage("WidgetWithInvalidScope");

    new ViewProxy<Widget>(new WidgetWithInvalidScope(), userSession);
  }

  @Test
  public void widgetShouldRequireMandatoryProperties() {
    ViewProxy<Widget> proxy = new ViewProxy<Widget>(new EditableWidget(), userSession);
    assertThat(proxy.hasRequiredProperties()).isTrue();
  }

  @Test
  public void widgetShouldDefineOnlyOptionalProperties() {
    ViewProxy<Widget> proxy = new ViewProxy<Widget>(new WidgetWithOptionalProperties(), userSession);
    assertThat(proxy.hasRequiredProperties()).isFalse();
  }

  @Test
  public void shouldAcceptAvailableMeasuresForNoRequiredMeasures() {
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }
    ViewProxy<?> proxy = new ViewProxy<>(new MyView(), userSession);

    assertThat(proxy.acceptsAvailableMeasures(new String[] {"lines", "ncloc", "coverage"})).isTrue();
  }

  @Test
  public void shouldAcceptAvailableMeasuresForMandatoryMeasures() {
    @RequiredMeasures(allOf = {"lines", "ncloc"})
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }
    ViewProxy<?> proxy = new ViewProxy<>(new MyView(), userSession);

    assertThat(proxy.acceptsAvailableMeasures(new String[] {"lines", "ncloc", "coverage"})).isTrue();
    assertThat(proxy.acceptsAvailableMeasures(new String[] {"lines", "coverage"})).isFalse();
  }

  @Test
  public void shouldAcceptAvailableMeasuresForOneOfNeededMeasures() {
    @RequiredMeasures(anyOf = {"lines", "ncloc"})
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }
    ViewProxy<?> proxy = new ViewProxy<>(new MyView(), userSession);

    assertThat(proxy.acceptsAvailableMeasures(new String[] {"lines", "coverage"})).isTrue();
    assertThat(proxy.acceptsAvailableMeasures(new String[] {"complexity", "coverage"})).isFalse();
  }

  @Test
  public void shouldAcceptAvailableMeasuresForMandatoryAndOneOfNeededMeasures() {
    @RequiredMeasures(allOf = {"lines", "ncloc"}, anyOf = {"duplications", "duplictated_blocks"})
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }
    ViewProxy<?> proxy = new ViewProxy<>(new MyView(), userSession);

    // ok, mandatory measures and 1 needed measure
    assertThat(proxy.acceptsAvailableMeasures(new String[] {"lines", "ncloc", "coverage", "duplications"})).isTrue();
    // ko, one of the needed measures but not all of the mandatory ones
    assertThat(proxy.acceptsAvailableMeasures(new String[] {"lines", "coverage", "duplications"})).isFalse();
    // ko, mandatory measures but no one of the needed measures
    assertThat(proxy.acceptsAvailableMeasures(new String[] {"lines", "nloc", "coverage", "dsm"})).isFalse();
  }

  @Test
  public void is_authorized_by_default() {

    @NavigationSection(NavigationSection.HOME)
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }

    ViewProxy<?> proxy = new ViewProxy<>(new MyView(), userSession);

    assertThat(proxy.isUserAuthorized()).isTrue();
  }

  @Test
  public void is_authorized_on_any_permission() {

    @NavigationSection(NavigationSection.HOME)
    @UserRole({"polop", "palap"})
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }

    ViewProxy<?> proxy = new ViewProxy<>(new MyView(), userSession);

    userSession.setGlobalPermissions("palap");
    assertThat(proxy.isUserAuthorized()).isTrue();
  }

  @Test
  public void is_not_authorized() {

    @NavigationSection(NavigationSection.HOME)
    @UserRole({"polop", "palap"})
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }

    ViewProxy<?> proxy = new ViewProxy<>(new MyView(), userSession);

    userSession.setGlobalPermissions("pilip");
    assertThat(proxy.isUserAuthorized()).isFalse();
  }

  @Test
  public void is_authorized_by_default_on_component() {

    @NavigationSection(NavigationSection.RESOURCE)
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }

    ViewProxy<?> proxy = new ViewProxy<>(new MyView(), userSession);

    assertThat(proxy.isUserAuthorized(newProjectDto("abcd"))).isTrue();
  }

  @Test
  public void is_authorized_on_any_permission_on_component() {

    @NavigationSection(NavigationSection.RESOURCE)
    @UserRole({"polop", "palap"})
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }

    ViewProxy<?> proxy = new ViewProxy<>(new MyView(), userSession);

    userSession.addProjectUuidPermissions("palap", "abcd");
    assertThat(proxy.isUserAuthorized(newProjectDto("abcd"))).isTrue();
  }

  @Test
  public void is_not_authorized_on_component() {

    @NavigationSection(NavigationSection.RESOURCE)
    @UserRole({"polop", "palap"})
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }

    ViewProxy<?> proxy = new ViewProxy<>(new MyView(), userSession);

    userSession.addProjectUuidPermissions("pilip", "abcd");
    assertThat(proxy.isUserAuthorized(newProjectDto("abcd"))).isFalse();
  }
}

class FakeView implements View {

  private String id;

  FakeView(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getTitle() {
    return id;
  }
}

@WidgetProperties({
  @WidgetProperty(key = "first_prop", optional = false),
  @WidgetProperty(key = "second_prop", defaultValue = "30", type = WidgetPropertyType.INTEGER),
  @WidgetProperty(key = "third_prop", type = WidgetPropertyType.INTEGER)
})
class EditableWidget implements Widget {
  @Override
  public String getId() {
    return "w1";
  }

  @Override
  public String getTitle() {
    return "W1";
  }
}

@WidgetProperties(@WidgetProperty(key = "message", defaultValue = "", type = WidgetPropertyType.TEXT))
class TextWidget implements Widget {
  @Override
  public String getId() {
    return "text";
  }

  @Override
  public String getTitle() {
    return "TEXT";
  }
}

@WidgetScope("GLOBAL")
class GlobalWidget implements Widget {
  @Override
  public String getId() {
    return "global";
  }

  @Override
  public String getTitle() {
    return "Global";
  }
}

@WidgetScope("INVALID")
class WidgetWithInvalidScope implements Widget {
  @Override
  public String getId() {
    return "invalidScope";
  }

  @Override
  public String getTitle() {
    return "InvalidScope";
  }
}

@WidgetProperties({
  @WidgetProperty(key = "foo"),
  @WidgetProperty(key = "bar")
})
class WidgetWithOptionalProperties implements Widget {
  @Override
  public String getId() {
    return "w2";
  }

  @Override
  public String getTitle() {
    return "W2";
  }
}
