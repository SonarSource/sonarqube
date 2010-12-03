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
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.Page;
import org.sonar.api.web.View;
import org.sonar.api.web.Widget;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ViewsTest {

  private static final View[] VIEWS = {new FakePage(), new FakeResourceViewer(), new FakeWidget()};

  @Test
  public void getPageById() {
    final Views views = new Views(VIEWS);
    assertThat(views.getPage("fake-page").getTarget(), is(FakePage.class));
    assertThat(views.getPage("fake-widget"), nullValue());
    assertThat(views.getPage("foo"), nullValue());
    assertThat(views.getPage("fake-resourceviewer").getTarget(), is(FakeResourceViewer.class));
  }

  @Test
  public void getPagesBySection() {
    final Views views = new Views(VIEWS);

    List<ViewProxy<Page>> pages = views.getPages(NavigationSection.RESOURCE);
    assertThat(pages.size(), is(1));
    assertThat(pages.get(0).getTarget(), is(FakePage.class));

    pages = views.getPages(NavigationSection.CONFIGURATION);
    assertThat(pages.size(), is(0));
  }

  @Test
  public void getResourceViewers() {
    final Views views = new Views(VIEWS);
    List<ViewProxy<Page>> resourceViewers = views.getPages(NavigationSection.RESOURCE_TAB);
    assertThat(resourceViewers.size(), is(1));
    assertThat(resourceViewers.get(0).getTarget(), is(FakeResourceViewer.class));
  }

  @Test
  public void getWidgets() {
    final Views views = new Views(VIEWS);
    List<ViewProxy<Widget>> widgets = views.getWidgets(null, null, null);
    assertThat(widgets.size(), is(1));
    assertThat(widgets.get(0).getTarget(), is(FakeWidget.class));
  }

  @Test
  public void sortViewsByTitle() {
    final Views views = new Views(new View[]{new FakeWidget("ccc", "ccc"), new FakeWidget("aaa", "aaa"), new FakeWidget("bbb", "bbb")});
    List<ViewProxy<Widget>> widgets = views.getWidgets(null, null, null);
    assertThat(widgets.size(), is(3));
    assertThat(widgets.get(0).getId(), is("aaa"));
    assertThat(widgets.get(1).getId(), is("bbb"));
    assertThat(widgets.get(2).getId(), is("ccc"));
  }

  @Test
  public void prefixTitleByNumberToDisplayFirst() {
    final Views views = new Views(new View[]{new FakeWidget("other", "Other"), new FakeWidget("1id", "1widget"), new FakeWidget("2id", "2widget")});
    List<ViewProxy<Widget>> widgets = views.getWidgets(null, null, null);
    assertThat(widgets.size(), is(3));
    assertThat(widgets.get(0).getId(), is("1id"));
    assertThat(widgets.get(1).getId(), is("2id"));
    assertThat(widgets.get(2).getId(), is("other"));
  }

  @Test
  public void acceptNavigationSection() {
    ViewProxy proxy = mock(ViewProxy.class);
    when(proxy.getSections()).thenReturn(new String[]{NavigationSection.RESOURCE});
    when(proxy.isWidget()).thenReturn(false);

    assertThat(Views.acceptNavigationSection(proxy, NavigationSection.RESOURCE), is(true));
    assertThat(Views.acceptNavigationSection(proxy, NavigationSection.HOME), is(false));
    assertThat(Views.acceptNavigationSection(proxy, NavigationSection.CONFIGURATION), is(false));
    assertThat(Views.acceptNavigationSection(proxy, null), is(true));
  }

  @Test
  public void doNotCheckNavigationSectionOnWidgets() {
    ViewProxy proxy = mock(ViewProxy.class);
    when(proxy.isWidget()).thenReturn(true);

    assertThat(Views.acceptNavigationSection(proxy, NavigationSection.RESOURCE), is(true));
    assertThat(Views.acceptNavigationSection(proxy, NavigationSection.HOME), is(true));
    assertThat(Views.acceptNavigationSection(proxy, NavigationSection.CONFIGURATION), is(true));
    assertThat(Views.acceptNavigationSection(proxy, null), is(true));
  }

  @Test
  public void checkResourceLanguage() {
    ViewProxy proxy = mock(ViewProxy.class);
    assertThat(Views.acceptResourceLanguage(proxy, Java.KEY), is(true));

    when(proxy.getResourceLanguages()).thenReturn(new String[]{"foo"});
    assertThat(Views.acceptResourceLanguage(proxy, Java.KEY), is(false));
    assertThat(Views.acceptResourceLanguage(proxy, "foo"), is(true));
  }

  @Test
  public void checkResourceScope() {
    ViewProxy proxy = mock(ViewProxy.class);
    assertThat(Views.acceptResourceScope(proxy, Project.SCOPE_ENTITY), is(true));

    when(proxy.getResourceScopes()).thenReturn(new String[]{Project.SCOPE_SET, Project.SCOPE_ENTITY});
    assertThat(Views.acceptResourceScope(proxy, Project.SCOPE_ENTITY), is(true));
    assertThat(Views.acceptResourceScope(proxy, Project.SCOPE_SPACE), is(false));
  }

  @Test
  public void checkResourceQualifier() {
    ViewProxy proxy = mock(ViewProxy.class);
    assertThat(Views.acceptResourceQualifier(proxy, Project.SCOPE_ENTITY), is(true));

    when(proxy.getResourceQualifiers()).thenReturn(new String[]{Project.QUALIFIER_CLASS, Project.QUALIFIER_FILE});
    assertThat(Views.acceptResourceQualifier(proxy, Project.QUALIFIER_FILE), is(true));
    assertThat(Views.acceptResourceQualifier(proxy, Project.QUALIFIER_PACKAGE), is(false));
  }
}