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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.ArrayUtils;
import org.sonar.api.ServerSide;
import org.sonar.api.web.Page;
import org.sonar.api.web.View;
import org.sonar.api.web.Widget;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

@ServerSide
public class Views {

  private Map<String, ViewProxy<Page>> pagesPerId = Maps.newHashMap();
  private Set<ViewProxy<Page>> pages = Sets.newTreeSet();

  private Map<String, ViewProxy<Widget>> widgetsPerId = Maps.newHashMap();
  private Set<ViewProxy<Widget>> widgets = Sets.newTreeSet();

  public Views() {
  }

  public Views(View[] views) {
    for (View view : views) {
      register(view);
    }
  }

  private void register(View view) {
    if (view instanceof Widget) {
      ViewProxy<Widget> proxy = new ViewProxy<Widget>((Widget) view);
      widgets.add(proxy);
      widgetsPerId.put(proxy.getId(), proxy);

    } else if (view instanceof Page) {
      ViewProxy<Page> proxy = new ViewProxy<Page>((Page) view);
      pagesPerId.put(proxy.getId(), proxy);
      pages.add(proxy);
    }
  }

  public ViewProxy<Page> getPage(String id) {
    return pagesPerId.get(id);
  }

  public List<ViewProxy<Page>> getPages(String section) {
    return getPages(section, null, null, null, null);
  }

  public List<ViewProxy<Page>> getPages(String section,
    @Nullable String resourceScope, @Nullable String resourceQualifier, @Nullable String resourceLanguage, @Nullable String[] availableMeasures) {
    List<ViewProxy<Page>> result = Lists.newArrayList();
    for (ViewProxy<Page> proxy : pages) {
      if (accept(proxy, section, resourceScope, resourceQualifier, resourceLanguage, availableMeasures)) {
        result.add(proxy);
      }
    }
    return result;
  }

  public List<ViewProxy<Page>> getPagesForMetric(String section, String resourceScope, String resourceQualifier, String resourceLanguage,
    String[] availableMeasures, String metric) {
    List<ViewProxy<Page>> result = Lists.newArrayList();
    for (ViewProxy<Page> proxy : pages) {
      if (accept(proxy, section, resourceScope, resourceQualifier, resourceLanguage, availableMeasures) && proxy.supportsMetric(metric)) {
        result.add(proxy);
      }
    }
    return result;
  }

  public ViewProxy<Widget> getWidget(String id) {
    return widgetsPerId.get(id);
  }

  public List<ViewProxy<Widget>> getWidgets(String resourceScope, String resourceQualifier, String resourceLanguage, String[] availableMeasures) {
    List<ViewProxy<Widget>> result = Lists.newArrayList();
    for (ViewProxy<Widget> proxy : widgets) {
      if (accept(proxy, null, resourceScope, resourceQualifier, resourceLanguage, availableMeasures)) {
        result.add(proxy);
      }
    }
    return result;
  }

  public List<ViewProxy<Widget>> getWidgets() {
    return Lists.newArrayList(widgets);
  }

  protected static boolean accept(ViewProxy proxy,
    @Nullable String section, @Nullable String resourceScope, @Nullable String resourceQualifier, @Nullable String resourceLanguage, @Nullable String[] availableMeasures) {
    return acceptNavigationSection(proxy, section)
      && acceptResourceScope(proxy, resourceScope)
      && acceptResourceQualifier(proxy, resourceQualifier)
      && acceptResourceLanguage(proxy, resourceLanguage)
      && acceptAvailableMeasures(proxy, availableMeasures);
  }

  protected static boolean acceptResourceLanguage(ViewProxy proxy, @Nullable String resourceLanguage) {
    return resourceLanguage == null || ArrayUtils.isEmpty(proxy.getResourceLanguages()) || ArrayUtils.contains(proxy.getResourceLanguages(), resourceLanguage);
  }

  protected static boolean acceptResourceScope(ViewProxy proxy, @Nullable String resourceScope) {
    return resourceScope == null || ArrayUtils.isEmpty(proxy.getResourceScopes()) || ArrayUtils.contains(proxy.getResourceScopes(), resourceScope);
  }

  protected static boolean acceptResourceQualifier(ViewProxy proxy, @Nullable String resourceQualifier) {
    return resourceQualifier == null || ArrayUtils.isEmpty(proxy.getResourceQualifiers()) || ArrayUtils.contains(proxy.getResourceQualifiers(), resourceQualifier);
  }

  protected static boolean acceptNavigationSection(ViewProxy proxy, @Nullable String section) {
    return proxy.isWidget() || section == null || ArrayUtils.contains(proxy.getSections(), section);
  }

  protected static boolean acceptAvailableMeasures(ViewProxy proxy, @Nullable String[] availableMeasures) {
    return availableMeasures == null || proxy.acceptsAvailableMeasures(availableMeasures);
  }
}
