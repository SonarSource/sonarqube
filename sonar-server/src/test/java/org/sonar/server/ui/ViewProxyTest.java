/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
import org.sonar.api.web.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

public class ViewProxyTest {

  @Test
  public void compareTo() {
    assertThat(new ViewProxy(new FakeView("aaa")).compareTo(new ViewProxy(new FakeView("bbb"))), lessThan(0));
    assertThat(new ViewProxy(new FakeView("aaa")).compareTo(new ViewProxy(new FakeView("aaa"))), is(0));
    assertThat(new ViewProxy(new FakeView("bbb")).compareTo(new ViewProxy(new FakeView("aaa"))), greaterThan(0));
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
    ViewProxy proxy = new ViewProxy(view);

    assertThat(proxy.getTarget(), is(view));
    assertArrayEquals(proxy.getSections(), new String[]{NavigationSection.RESOURCE});
    assertArrayEquals(proxy.getUserRoles(), new String[]{UserRole.ADMIN});
  }

  @Test
  public void doLoadDefaultMetadata() {

    class MyView extends FakeView {
      MyView() {
        super("fake");
      }
    }
    View view = new MyView();
    ViewProxy proxy = new ViewProxy(view);

    assertThat(proxy.getTarget(), is(view));
    assertArrayEquals(proxy.getSections(), new String[]{NavigationSection.HOME});
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
    assertThat(proxy.getDefaultTabForMetrics(), is(new String[]{"ncloc", "coverage"}));
  }

  @Test
  public void widgetShouldBeEditable() {
    ViewProxy proxy = new ViewProxy<Widget>(new EditableWidget());
    assertThat(proxy.isEditable(), is(true));
    assertThat(proxy.getProperties().length, is(2));
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
    @WidgetProperty(key="foo", optional = false),
    @WidgetProperty(key="bar", defaultValue = "30", type = "INTEGER")
})
class EditableWidget implements Widget {

  public String getId() {
    return "w1";
  }

  public String getTitle() {
    return "W1";
  }
}

@WidgetProperties({
    @WidgetProperty(key="foo"),
    @WidgetProperty(key="bar")
})
class WidgetWithOptionalProperties implements Widget {

  public String getId() {
    return "w2";
  }

  public String getTitle() {
    return "W2";
  }
}
