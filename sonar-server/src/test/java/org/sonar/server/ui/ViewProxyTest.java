/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.ui;

import org.junit.Test;
import org.sonar.api.web.DefaultTab;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.RequiredMeasures;
import org.sonar.api.web.UserRole;
import org.sonar.api.web.View;
import org.sonar.api.web.Widget;
import org.sonar.api.web.WidgetCategory;
import org.sonar.api.web.WidgetProperties;
import org.sonar.api.web.WidgetProperty;
import org.sonar.api.web.WidgetPropertyType;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

public class ViewProxyTest {

  @Test
  public void compareTo() {
    assertThat(new ViewProxy<FakeView>(new FakeView("aaa")).compareTo(new ViewProxy<FakeView>(new FakeView("bbb"))), lessThan(0));
    assertThat(new ViewProxy<FakeView>(new FakeView("aaa")).compareTo(new ViewProxy<FakeView>(new FakeView("aaa"))), is(0));
    assertThat(new ViewProxy<FakeView>(new FakeView("bbb")).compareTo(new ViewProxy<FakeView>(new FakeView("aaa"))), greaterThan(0));
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
    ViewProxy proxy = new ViewProxy<View>(view);

    assertThat(proxy.getTarget(), is(view));
    assertArrayEquals(proxy.getSections(), new String[] {NavigationSection.RESOURCE});
    assertArrayEquals(proxy.getUserRoles(), new String[] {UserRole.ADMIN});
  }

  @Test
  public void doLoadDefaultMetadata() {

    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }
    View view = new MyView();
    ViewProxy proxy = new ViewProxy<View>(view);

    assertThat(proxy.getTarget(), is(view));
    assertArrayEquals(proxy.getSections(), new String[] {NavigationSection.HOME});
    assertThat(proxy.getUserRoles().length, org.hamcrest.Matchers.is(0));
  }

  @Test
  public void isDefaultTab() {
    @DefaultTab
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }

    ViewProxy proxy = new ViewProxy<MyView>(new MyView());

    assertThat(proxy.isDefaultTab(), is(true));
    assertThat(proxy.getDefaultTabForMetrics().length, is(0));
  }

  @Test
  public void isNotDefaultTab() {
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }
    ViewProxy proxy = new ViewProxy<MyView>(new MyView());

    assertThat(proxy.isDefaultTab(), is(false));
    assertThat(proxy.getDefaultTabForMetrics().length, is(0));
  }

  @Test
  public void isDefaultTabForMetrics() {
    @DefaultTab(metrics = {"ncloc", "coverage"})
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }
    ViewProxy proxy = new ViewProxy<MyView>(new MyView());

    assertThat(proxy.isDefaultTab(), is(false));
    assertThat(proxy.getDefaultTabForMetrics(), is(new String[] {"ncloc", "coverage"}));
  }

  @Test
  public void widget_should_be_editable() {
    ViewProxy proxy = new ViewProxy<Widget>(new EditableWidget());

    assertThat(proxy.isEditable()).isTrue();
    assertThat(proxy.getWidgetProperties()).hasSize(2);
  }

  @Test
  public void widget_should_not_be_detached() {
    ViewProxy proxy = new ViewProxy<Widget>(new EditableWidget());

    assertThat(proxy.isDetached()).isFalse();
  }

  @Test
  public void widget_should_be_detached() {
    ViewProxy proxy = new ViewProxy<Widget>(new DetachedWidget());

    assertThat(proxy.isDetached()).isTrue();
  }

  @Test
  public void widgetShouldRequireMandatoryProperties() {
    ViewProxy proxy = new ViewProxy<Widget>(new EditableWidget());
    assertThat(proxy.hasRequiredProperties(), is(true));
  }

  @Test
  public void widgetShouldDefineOnlyOptionalProperties() {
    ViewProxy proxy = new ViewProxy<Widget>(new WidgetWithOptionalProperties());
    assertThat(proxy.hasRequiredProperties(), is(false));
  }

  @Test
  public void shouldAcceptAvailableMeasuresForNoRequiredMeasures() throws Exception {
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }
    ViewProxy proxy = new ViewProxy<MyView>(new MyView());

    assertThat(proxy.acceptsAvailableMeasures(new String[] {"lines", "ncloc", "coverage"}), is(true));
  }

  @Test
  public void shouldAcceptAvailableMeasuresForMandatoryMeasures() throws Exception {
    @RequiredMeasures(allOf = {"lines", "ncloc"})
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }
    ViewProxy proxy = new ViewProxy<MyView>(new MyView());

    assertThat(proxy.acceptsAvailableMeasures(new String[] {"lines", "ncloc", "coverage"}), is(true));
    assertThat(proxy.acceptsAvailableMeasures(new String[] {"lines", "coverage"}), is(false));
  }

  @Test
  public void shouldAcceptAvailableMeasuresForOneOfNeededMeasures() throws Exception {
    @RequiredMeasures(anyOf = {"lines", "ncloc"})
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }
    ViewProxy proxy = new ViewProxy<MyView>(new MyView());

    assertThat(proxy.acceptsAvailableMeasures(new String[] {"lines", "coverage"}), is(true));
    assertThat(proxy.acceptsAvailableMeasures(new String[] {"complexity", "coverage"}), is(false));
  }

  @Test
  public void shouldAcceptAvailableMeasuresForMandatoryAndOneOfNeededMeasures() throws Exception {
    @RequiredMeasures(allOf = {"lines", "ncloc"}, anyOf = {"duplications", "duplictated_blocks"})
    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }
    ViewProxy proxy = new ViewProxy<MyView>(new MyView());

    // ok, mandatory measures and 1 needed measure
    assertThat(proxy.acceptsAvailableMeasures(new String[] {"lines", "ncloc", "coverage", "duplications"}), is(true));
    // ko, one of the needed measures but not all of the mandatory ones
    assertThat(proxy.acceptsAvailableMeasures(new String[] {"lines", "coverage", "duplications"}), is(false));
    // ko, mandatory measures but no one of the needed measures
    assertThat(proxy.acceptsAvailableMeasures(new String[] {"lines", "nloc", "coverage", "dsm"}), is(false));
  }

}

class FakeView implements View {

  private String id;

  FakeView(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public String getTitle() {
    return id;
  }
}

@WidgetProperties({
  @WidgetProperty(key = "foo", optional = false),
  @WidgetProperty(key = "bar", defaultValue = "30", type = WidgetPropertyType.INTEGER)
})
class EditableWidget implements Widget {

  public String getId() {
    return "w1";
  }

  public String getTitle() {
    return "W1";
  }
}

@WidgetCategory(value = "", detached = true)
class DetachedWidget implements Widget {
  public String getId() {
    return "detached";
  }

  public String getTitle() {
    return "Detached";
  }
}

@WidgetProperties({
  @WidgetProperty(key = "foo"),
  @WidgetProperty(key = "bar")
})
class WidgetWithOptionalProperties implements Widget {

  public String getId() {
    return "w2";
  }

  public String getTitle() {
    return "W2";
  }
}
