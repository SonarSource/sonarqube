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
package org.sonar.server.ui.ws;

import org.sonar.api.config.Settings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.Page;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.plugins.UpdateCenterClient;
import org.sonar.server.ui.ViewProxy;
import org.sonar.server.ui.Views;
import org.sonar.server.user.UserSession;

public class SettingsNavigationAction implements NavigationWsAction {

  private final Settings settings;
  private final Views views;
  private final I18n i18n;
  private final UserSession userSession;

  public SettingsNavigationAction(Settings settings, Views views, I18n i18n, UserSession userSession) {
    this.views = views;
    this.settings = settings;
    this.i18n = i18n;
    this.userSession = userSession;
  }

  @Override
  public void define(NewController context) {
    context.createAction("settings")
      .setDescription("Get configuration information for the settings page:" +
        "<ul>" +
        "  <li>List plugin-contributed pages</li>" +
        "  <li>Show update center (or not)</li>" +
        "</ul>")
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource("example-settings.json"))
      .setSince("5.2");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    boolean isAdmin = userSession.hasGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);

    JsonWriter json = response.newJsonWriter().beginObject();
    json.prop("showUpdateCenter", isAdmin && settings.getBoolean(UpdateCenterClient.ACTIVATION_PROPERTY));
    json.prop("showProvisioning", userSession.hasGlobalPermission(GlobalPermissions.PROVISIONING));

    json.name("extensions").beginArray();
    if (isAdmin) {
      for (ViewProxy<Page> page : views.getPages(NavigationSection.CONFIGURATION, null, null, null, null)) {
        json.beginObject()
          .prop("name", i18n.message(userSession.locale(), String.format("%s.page", page.getTitle()), page.getTitle()))
          .prop("url", getPageUrl(page))
          .endObject();
      }
    }
    json.endArray();

    json.endObject().close();
  }

  private static String getPageUrl(ViewProxy<Page> page) {
    return page.isController() ? page.getId() : String.format("/plugins/configuration/%s", page.getId());
  }
}
