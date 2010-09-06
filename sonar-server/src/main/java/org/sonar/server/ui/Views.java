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

import org.apache.commons.lang.ArrayUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.web.Page;
import org.sonar.api.web.View;
import org.sonar.api.web.Widget;

import java.util.*;

public class Views implements ServerComponent {

  private Map<String, ViewProxy<Page>> pagesPerId = new HashMap<String, ViewProxy<Page>>();
  private Set<ViewProxy<Page>> pages = new TreeSet<ViewProxy<Page>>();

  private Map<String, ViewProxy<Widget>> widgetsPerId = new HashMap<String, ViewProxy<Widget>>();
  private Set<ViewProxy<Widget>> widgets = new TreeSet<ViewProxy<Widget>>();

  public Views() {
  }

  public Views(View[] views) {
    for (View view : views) {
      ViewProxy proxy = new ViewProxy(view);
      if (view instanceof Widget) {
        widgets.add(proxy);
        widgetsPerId.put(proxy.getId(), proxy);

      } else if (view instanceof Page) {
        pagesPerId.put(proxy.getId(), proxy);
        pages.add(proxy);
      }
    }
  }


  public ViewProxy<Page> getPage(String id) {
    return pagesPerId.get(id);
  }

  public List<ViewProxy<Page>> getPages(String section) {
    return getPages(section, null, null, null);
  }

  public List<ViewProxy<Page>> getPages(String section, String resourceScope, String resourceQualifier, String resourceLanguage) {
    List<ViewProxy<Page>> result = new ArrayList<ViewProxy<Page>>();
    for (ViewProxy<Page> proxy : pages) {
      if (accept(proxy, section, resourceScope, resourceQualifier, resourceLanguage)) {
        result.add(proxy);
      }
    }
    return result;
  }

  public ViewProxy<Widget> getWidget(String id) {
    return widgetsPerId.get(id);
  }

  public List<ViewProxy<Widget>> getWidgets(String resourceScope, String resourceQualifier, String resourceLanguage) {
    List<ViewProxy<Widget>> result = new ArrayList<ViewProxy<Widget>>();
    for (ViewProxy<Widget> proxy : widgets) {
      if (accept(proxy, null, resourceScope, resourceQualifier, resourceLanguage)) {
        result.add(proxy);
      }
    }
    return result;
  }

  private static boolean accept(ViewProxy proxy, String section, String resourceScope, String resourceQualifier, String resourceLanguage) {
    return acceptNavigationSection(proxy, section)
        && acceptResourceScope(proxy, resourceScope)
        && acceptResourceQualifier(proxy, resourceQualifier)
        && acceptResourceLanguage(proxy, resourceLanguage);
  }

  protected static boolean acceptResourceLanguage(ViewProxy proxy, String resourceLanguage) {
    return resourceLanguage== null || ArrayUtils.isEmpty(proxy.getResourceLanguages()) || ArrayUtils.contains(proxy.getResourceLanguages(), resourceLanguage);
  }

  protected static boolean acceptResourceScope(ViewProxy proxy, String resourceScope) {
    return resourceScope== null || ArrayUtils.isEmpty(proxy.getResourceScopes()) || ArrayUtils.contains(proxy.getResourceScopes(), resourceScope);
  }

  protected static boolean acceptResourceQualifier(ViewProxy proxy, String resourceQualifier) {
    return resourceQualifier== null || ArrayUtils.isEmpty(proxy.getResourceQualifiers()) || ArrayUtils.contains(proxy.getResourceQualifiers(), resourceQualifier);
  }

  protected static boolean acceptNavigationSection(ViewProxy proxy, String section) {
    return proxy.isWidget() || section == null || ArrayUtils.contains(proxy.getSections(), section);
  }
}
