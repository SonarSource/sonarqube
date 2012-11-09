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
package org.sonar.gwt.ui;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import org.sonar.wsclient.gwt.GwtUtils;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.WSUtils;
import org.sonar.wsclient.unmarshallers.ResourceUnmarshaller;

public abstract class Page implements EntryPoint {

  private static final ResourceUnmarshaller RESOURCE_UNMARSHALLER = new ResourceUnmarshaller();

  public final void onModuleLoad() {
    export(GWT.getModuleName(), this);
    load();
    onResourceLoad();
  }

  private void load() {
    Widget widget = doOnModuleLoad();
    if (widget != null) {
      getRootPanel().add(widget);
    }
  }

  protected Widget doOnModuleLoad() {
    return null;
  }

  public final void onResourceLoad() {
    JavaScriptObject json = loadResource();
    if (json != null) {
      if (WSUtils.getINSTANCE() == null) {
        WSUtils.setInstance(new GwtUtils()); // TODO dirty hack to initialize WSUtils
      }
      String jsonStr = (new JSONObject(json)).toString();
      Resource resource = RESOURCE_UNMARSHALLER.toModel(jsonStr);

      RootPanel container = getRootPanel();
      container.clear();

      Widget currentWidget = doOnResourceLoad(resource);
      if (currentWidget != null) {
        container.add(currentWidget);
      }
    }
  }

  protected Widget doOnResourceLoad(Resource resource) {
    return null;
  }

  protected final RootPanel getRootPanel() {
    RootPanel result = RootPanel.get("gwtpage-" + GWT.getModuleName());
    if (result == null) {
      result = RootPanel.get("gwtpage");
    }
    return result;
  }

  private native JavaScriptObject loadResource()/*-{
                                                return $wnd.config['resource'];
                                                }-*/;

  private native void export(String gwtId, Page page)/*-{
                                                     $wnd.modules[gwtId]=function() {page.@org.sonar.gwt.ui.Page::onResourceLoad()()};
                                                     }-*/;
}
