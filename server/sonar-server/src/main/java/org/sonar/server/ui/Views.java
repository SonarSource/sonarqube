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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang.ArrayUtils;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.Page;
import org.sonar.api.web.View;
import org.sonar.api.web.Widget;
import org.sonar.server.user.UserSession;

@ServerSide
public class Views {

  private Map<String, ViewProxy<Page>> pagesPerId = Maps.newHashMap();
  private Set<ViewProxy<Page>> pages = Sets.newTreeSet();

  private Map<String, ViewProxy<Widget>> widgetsPerId = Maps.newHashMap();
  private Set<ViewProxy<Widget>> widgets = Sets.newTreeSet();

  private final UserSession userSession;

  public Views(UserSession userSession) {
    this.userSession = userSession;
  }

  public Views(UserSession userSession, View[] views) {
    this.userSession = userSession;
    for (View view : views) {
      register(view);
    }
  }

  private void register(View view) {
    if (view instanceof Widget) {
      ViewProxy<Widget> proxy = new ViewProxy<>((Widget) view, userSession);
      widgets.add(proxy);
      widgetsPerId.put(proxy.getId(), proxy);

    } else if (view instanceof Page) {
      ViewProxy<Page> proxy = new ViewProxy<>((Page) view, userSession);
      pagesPerId.put(proxy.getId(), proxy);
      pages.add(proxy);
    }
  }

  public ViewProxy<Page> getPage(String id) {
    return pagesPerId.get(id);
  }

  public List<ViewProxy<Page>> getPages(String section) {
    List<ViewProxy<Page>> result = Lists.newArrayList();
    for (ViewProxy<Page> proxy : pages) {
      if (accept(proxy, section)) {
        result.add(proxy);
      }
    }
    return result;
  }

  public ViewProxy<Widget> getWidget(String id) {
    return widgetsPerId.get(id);
  }

  public List<ViewProxy<Widget>> getWidgets() {
    return Lists.newArrayList(widgets);
  }

  protected static boolean accept(ViewProxy<?> proxy, @Nullable String section) {
    return acceptNavigationSection(proxy, section);
  }

  protected static boolean acceptNavigationSection(ViewProxy<?> proxy, @Nullable String section) {
    return proxy.isWidget() || section == null || ArrayUtils.contains(proxy.getSections(), section);
  }
}
