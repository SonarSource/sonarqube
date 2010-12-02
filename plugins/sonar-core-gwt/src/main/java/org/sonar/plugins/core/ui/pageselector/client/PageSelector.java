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
package org.sonar.plugins.core.ui.pageselector.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.*;
import org.sonar.gwt.Configuration;
import org.sonar.gwt.Links;
import org.sonar.gwt.Utils;
import org.sonar.gwt.ui.Icons;
import org.sonar.gwt.ui.Loading;
import org.sonar.wsclient.gwt.Callback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.ArrayList;
import java.util.List;

public class PageSelector implements EntryPoint {

  public static final String GWT_ID = "org.sonar.plugins.core.ui.pageselector.PageSelector";
  public static final String HTML_ROOT_ID = "pageselector";

  private VerticalPanel container = null;
  private String currentResourceId = null;
  private PageDefs pageDefs = null;

  public void onModuleLoad() {
    pageDefs = loadPageDefs();
    exportNativeJavascript(this);
    if (Configuration.getResourceId() != null) {
      selectResource(Configuration.getResourceId());
    }
  }

  private VerticalPanel createContainer() {
    if (container == null) {
      container = new VerticalPanel();
      container.getElement().setId("rvs");
      RootPanel.get(HTML_ROOT_ID).add(container);
    }
    return container;
  }

  public static native void exportNativeJavascript(Object obj) /*-{
    $wnd.sr=function(resourceIdOrKey) {
      obj.@org.sonar.plugins.core.ui.pageselector.client.PageSelector::selectResource(Ljava/lang/String;)(resourceIdOrKey);
    };
  }-*/;

  public void selectResource(final String resourceIdOrKey) {
    createContainer().add(new Loading());
    currentResourceId = resourceIdOrKey;
    Sonar.getInstance().find(new ResourceQuery(resourceIdOrKey), new Callback<Resource>() {

      public void onResponse(Resource resource, JavaScriptObject json) {
        if (resourceIdOrKey != null && resourceIdOrKey.equals(currentResourceId)) {
          if (resource == null) {
            displayResourceNotFound();
          } else {
            saveResource(resource.getId().toString(), json);
            displayResource(resource);
          }
        } // else too late, user has selected another resource
      }

      public void onTimeout() {
        Utils.showError("Can not load data (timeout)");
      }

      public void onError(int errorCode, String errorMessage) {
        Utils.showError("Can not load data: error " + errorCode + ", message: " + errorMessage);
      }
    });
  }

  private void displayResource(final Resource resource) {
    List<PageDef> pages = selectPages(resource);

    PageDef selectedPage = selectPage(pages);

    Title title = new Title(resource);
    final TabPanel tabs = new TabPanel();
    tabs.setWidth("100%");

    int selectedTabIndex = -1;
    for (int tabIndex = 0; tabIndex < pages.size(); tabIndex++) {
      PageDef page = pages.get(tabIndex);
      tabs.add(new PagePanel(page), page.getName());
      if (page == selectedPage) {
        selectedTabIndex = tabIndex;
      }
    }

    container.clear(); // remove the loading icon
    container.add(title);
    container.add(tabs);

    tabs.addSelectionHandler(new SelectionHandler<Integer>() {
      public void onSelection(SelectionEvent<Integer> tabId) {
        ((PagePanel) tabs.getWidget(tabId.getSelectedItem())).display();
      }
    });

    if (selectedTabIndex > -1) {
      tabs.selectTab(selectedTabIndex);
    }
  }

  private PageDef selectPage(List<PageDef> pages) {
    String pageId = Configuration.getRequestParameter("page");
    if (pageId != null) {
      for (PageDef page : pages) {
        if (pageId.equals(page.getId())) {
          return page;
        }
      }
    }
    String metric = Configuration.getParameter("metric");
    if (metric != null) {
      for (PageDef page : pages) {
        if (page.acceptMetric(metric)) {
          return page;
        }
      }
    }

    for (PageDef page : pages) {
      if (page.isDefaultTab()) {
        return page;
      }
    }

    return null;
  }

  private native void saveResource(String resourceId, JavaScriptObject json) /*-{
    $wnd.config['resource_key']=resourceId;
    $wnd.config['resource']=json;
  }-*/;


  /**
   * Never return null.
   */
  private List<PageDef> selectPages(final Resource resource) {
    List<PageDef> pages = new ArrayList<PageDef>();
    for (int index = 0; index < pageDefs.length(); index++) {
      PageDef page = pageDefs.get(index);
      if (page.acceptLanguage(resource.getLanguage()) &&
          page.acceptQualifier(resource.getQualifier()) &&
          page.acceptScope(resource.getScope())) {
        pages.add(page);
      }
    }
    return pages;
  }

  private void displayResourceNotFound() {
    container.clear(); // remove the loading icon
  }


  static class Title extends Composite {
    Title(final Resource resource) {
      Grid grid = new Grid(1, 2);
      grid.getElement().setId("rvstitle");
      grid.setHTML(0, 0, Icons.forQualifier(resource.getQualifier()).getHTML() + " <span class='name'>" + resource.getName(true) + "</span>");

      if (!"true".equals(Configuration.getParameter("popup"))) {
        Hyperlink newWindow = new Hyperlink();
        newWindow.setText(I18nConstants.INSTANCE.newWindow());
        newWindow.setStyleName("command");
        newWindow.addClickHandler(new ClickHandler() {
          public void onClick(ClickEvent clickEvent) {
            Links.openMeasurePopup(resource.getKey(), Configuration.getParameter("metric"));
          }
        });
        grid.setWidget(0, 1, newWindow);
      }
      grid.getColumnFormatter().setStyleName(1, "right");
      initWidget(grid);
    }
  }


  private native PageDefs loadPageDefs() /*-{
    return $wnd.pages;
  }-*/;

  // An overlay type

  static class PageDefs extends JavaScriptObject {
    protected PageDefs() {
    }

    public final native int length() /*-{ return this.length; }-*/;

    public final native PageDef get(int i) /*-{ return this[i];     }-*/;
  }
}
