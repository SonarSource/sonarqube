/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.ui;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.web.DefaultTab;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.UserRole;
import org.sonar.api.web.View;
import org.sonar.server.tester.UserSessionRule;

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
