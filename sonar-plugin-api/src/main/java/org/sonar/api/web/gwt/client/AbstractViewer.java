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
package org.sonar.api.web.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import org.sonar.api.web.gwt.client.webservices.*;

import java.util.Arrays;

public abstract class AbstractViewer implements EntryPoint {

  public static final String HTML_ROOT_ID = "resource_viewers";

  private Resource resource;
  private String renderedResourceKey = "";
  private Panel widgetPanel = null;
  private boolean standAloneMode = true;

  public void onModuleLoad() {
    exportJavascript();
  }

  /**
   * Export GWT javascript methods to load and control the plugin, must export currently 2 method :
   * I.E for plugin GWT id : foo.bar.MyPlugin, class foo.bar.client.MyPlugin :
   * <p/>
   * $wnd.load_foo_bar_MyPlugin = function() {
   * called to the plugin init from JS
   * obj.@foo.bar.client.MyPlugin::loadContainer()();
   * }
   * $wnd.on_resource_loaded_foo_bar_MyPlugin = function() {
   * called when a resource JSON object has been refreshed within the page
   * obj.@foo.bar.client.MyPlugin::onResourceLoaded()();
   * }
   */
  protected abstract void exportJavascript();

  /**
   * When multiple widgets are bound to the same HTML div, this method will indicate
   * If the resource widget is the default one to show when the widget is initialized
   *
   * @param metric   the metric for which the widget is shown, cannot be null
   * @param resource the resource bound to the widget
   * @return true or false
   */
  protected abstract boolean isDefault(WSMetrics.Metric metric, Resource resource);

  /**
   * Finds if a given metric is in the provided metrics list
   *
   * @param metric      the metric to search
   * @param metricsList the metric list
   * @return true or false if not found
   */
  protected boolean isMetricInList(WSMetrics.Metric metric, WSMetrics.Metric... metricsList) {
    return Arrays.asList(metricsList).contains(metric);
  }

  /**
   * When multiple widgets are in the same page, this method will indicate if the widget
   * can be shown for the given resource
   *
   * @param resource the resource bound to the page
   * @return true or false
   */
  protected abstract boolean isForResource(Resource resource);


  public Resource getResource() {
    return resource;
  }

  private Resource loadResource() {
    JavaScriptObject resourceJson = getResourceJSONObject();
    if (resourceJson != null) {
      Resource resourceLoaded = ResourcesQuery.parseResources(resourceJson).get(0);
      String currentMetricKey = ResourceDictionary.getViewerMetricKey();
      Boolean isDefaultForMetric = false;
      if (currentMetricKey != null) {
        isDefaultForMetric = isDefault(WSMetrics.get(currentMetricKey), resourceLoaded);
      }
      exportJSBooleanVariable("is_default_for_metric", Utils.widgetGWTIdJSEncode(getGwtId()), isDefaultForMetric);
      exportJSBooleanVariable("is_for_resource", Utils.widgetGWTIdJSEncode(getGwtId()), isForResource(resourceLoaded));
      return resourceLoaded;
    }
    return null;
  }

  /**
   * Called when a resource JSON object has been loaded within the page
   */
  public final void onResourceLoaded() {
    resource = loadResource();
    standAloneMode = false;
  }

  /**
   * Called to render the widget for the given resource object loaded via the onResourceLoaded() method call
   */
  public final void loadContainer() {
    String resourceKey = ResourceDictionary.getViewerResourceKey();
    if (resourceKey != null) {
      if (!standAloneMode && resource == null) {
        Utils.showError("Unable to find JSON resource object, unable to render widget");
        return;
      } else if (standAloneMode && resource == null) {
        getResourceJsonObject(resourceKey);
        return;
      }
      String currentResourceKey = isANumber(resourceKey) ? resource.getId().toString() : resource.getKey();
      if (!renderedResourceKey.equals(currentResourceKey)) {
        // resource key has changed reload if not in standalone mode
        if (!standAloneMode) {
          resource = loadResource();
        }

        if (widgetPanel == null) {
          RootPanel rootPanel = RootPanel.get(HTML_ROOT_ID);
          if (rootPanel == null) {
            Utils.showError("Unable to find root panel " + HTML_ROOT_ID + " in page");
          }
          widgetPanel = new FlowPanel();
          widgetPanel.setStyleName("gwt-ResourceTab");
          String panelId = "tab-" + Utils.widgetGWTIdJSEncode(getGwtId());
          widgetPanel.getElement().setId(panelId);
          registerTab(panelId);
          widgetPanel.setVisible(false);
          rootPanel.add(widgetPanel);
        }

        renderedResourceKey = resourceKey;

        if (widgetPanel != null) {
          widgetPanel.clear();
          widgetPanel.add(render(resource));
        }
      }
    }

    if (widgetPanel != null) {
      widgetPanel.setVisible(true);
    }
  }

  private static native void registerTab(Object tabId) /*-{
    $wnd.registeredTabs.push(tabId);
  }-*/;


  private native void exportJSBooleanVariable(String varPrefix, String encodedGWTId, boolean value)/*-{
    $wnd.config[varPrefix + "_" + encodedGWTId] = value;
  }-*/;

  /**
   * Return the GWT id of the widget
   */
  protected abstract String getGwtId();

  /**
   * Renders the widget for the current resource
   */
  protected abstract Widget render(Resource resource);

  /**
   * Return a JavaScriptObject object containing all the measure available for the current resource key
   *
   * @return the JavaScriptObject instance, should never be null
   */
  protected native JavaScriptObject getResourceJSONObject()/*-{
    return $wnd.config['current_resource'];
  }-*/;


  private boolean isANumber(String resourceKey) {
    boolean isIdResourceKey = true;
    try {
      Integer.parseInt(resourceKey);
    } catch (NumberFormatException ex) {
      isIdResourceKey = false;
    }
    return isIdResourceKey;
  }

  private void getResourceJsonObject(String resourceKey) {
    ResourcesQuery.get(resourceKey).execute(new StandAloneResourceHandler());
  }

  public class StandAloneResourceHandler extends BaseQueryCallback<Resources> {
    public void onResponse(Resources resources, JavaScriptObject jsonResponse) {
      resource = resources.firstResource();
      loadContainer();
    }
  }
}
