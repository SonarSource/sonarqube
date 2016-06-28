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

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.Page;
import org.sonar.api.web.View;
import org.sonar.api.web.Widget;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ViewsTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private static FakeResourceViewer FAKE_TAB = new FakeResourceViewer();
  private static FakeWidget FAKE_WIDGET = new FakeWidget();
  private static FakePage FAKE_PAGE = new FakePage();
  private static final View[] VIEWS = {FAKE_PAGE, FAKE_TAB, FAKE_WIDGET};

  @Test
  public void should_get_page_by_id() {
    final Views views = new Views(userSessionRule, VIEWS);
    assertThat(views.getPage("fake-page").getTarget().getClass()).isEqualTo(FakePage.class);
    assertThat(views.getPage("fake-widget")).isNull();
    assertThat(views.getPage("foo")).isNull();
    assertThat(views.getPage("fake-resourceviewer").getTarget().getClass()).isEqualTo(FakeResourceViewer.class);
  }

  @Test
  public void should_get_pages_by_section() {
    final Views views = new Views(userSessionRule, VIEWS);

    List<ViewProxy<Page>> pages = views.getPages(NavigationSection.RESOURCE);
    assertThat(pages.size()).isEqualTo(1);
    assertThat(pages.get(0).getTarget().getClass()).isEqualTo(FakePage.class);

    pages = views.getPages(NavigationSection.CONFIGURATION);
    assertThat(pages.size()).isEqualTo(0);
  }

  @Test
  public void should_get_widgets() {
    final Views views = new Views(userSessionRule, VIEWS);
    List<ViewProxy<Widget>> widgets = views.getWidgets();
    assertThat(widgets.size()).isEqualTo(1);
    assertThat(widgets.get(0).getTarget().getClass()).isEqualTo(FakeWidget.class);
  }

  @Test
  public void should_sort_views_by_title() {
    final Views views = new Views(userSessionRule, new View[] {new FakeWidget("ccc", "ccc"), new FakeWidget("aaa", "aaa"), new FakeWidget("bbb", "bbb")});
    List<ViewProxy<Widget>> widgets = views.getWidgets();
    assertThat(widgets.size()).isEqualTo(3);
    assertThat(widgets.get(0).getId()).isEqualTo("aaa");
    assertThat(widgets.get(1).getId()).isEqualTo("bbb");
    assertThat(widgets.get(2).getId()).isEqualTo("ccc");
  }

  @Test
  public void should_prefix_title_by_number_to_display_first() {
    final Views views = new Views(userSessionRule, new View[] {new FakeWidget("other", "Other"), new FakeWidget("1id", "1widget"), new FakeWidget("2id", "2widget")});
    List<ViewProxy<Widget>> widgets = views.getWidgets();
    assertThat(widgets.size()).isEqualTo(3);
    assertThat(widgets.get(0).getId()).isEqualTo("1id");
    assertThat(widgets.get(1).getId()).isEqualTo("2id");
    assertThat(widgets.get(2).getId()).isEqualTo("other");
  }

  @Test
  public void should_accept_navigation_section() {
    ViewProxy<?> proxy = mock(ViewProxy.class);
    when(proxy.getSections()).thenReturn(new String[] {NavigationSection.RESOURCE});
    when(proxy.isWidget()).thenReturn(false);

    assertThat(Views.acceptNavigationSection(proxy, NavigationSection.RESOURCE)).isEqualTo(true);
    assertThat(Views.acceptNavigationSection(proxy, NavigationSection.HOME)).isEqualTo(false);
    assertThat(Views.acceptNavigationSection(proxy, NavigationSection.CONFIGURATION)).isEqualTo(false);
    assertThat(Views.acceptNavigationSection(proxy, NavigationSection.RESOURCE_CONFIGURATION)).isEqualTo(false);
    assertThat(Views.acceptNavigationSection(proxy, null)).isEqualTo(true);
  }

  @Test
  public void should_not_check_navigation_section_on_widgets() {
    ViewProxy<?> proxy = mock(ViewProxy.class);
    when(proxy.isWidget()).thenReturn(true);

    assertThat(Views.acceptNavigationSection(proxy, NavigationSection.RESOURCE)).isEqualTo(true);
    assertThat(Views.acceptNavigationSection(proxy, NavigationSection.HOME)).isEqualTo(true);
    assertThat(Views.acceptNavigationSection(proxy, NavigationSection.CONFIGURATION)).isEqualTo(true);
    assertThat(Views.acceptNavigationSection(proxy, NavigationSection.RESOURCE_CONFIGURATION)).isEqualTo(true);
    assertThat(Views.acceptNavigationSection(proxy, null)).isEqualTo(true);
  }
}
