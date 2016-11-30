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
package org.sonar.server.ui.ws;

import org.sonar.api.config.Settings;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.Page;
import org.sonar.server.ui.ViewProxy;
import org.sonar.server.ui.Views;

import static org.sonar.core.config.WebConstants.SONAR_LF_LOGO_URL;
import static org.sonar.core.config.WebConstants.SONAR_LF_LOGO_WIDTH_PX;

public class GlobalNavigationAction implements NavigationWsAction {

  private final Views views;
  private final Settings settings;
  private final ResourceTypes resourceTypes;

  public GlobalNavigationAction(Views views, Settings settings, ResourceTypes resourceTypes) {
    this.views = views;
    this.settings = settings;
    this.resourceTypes = resourceTypes;
  }

  @Override
  public void define(NewController context) {
    context.createAction("global")
      .setDescription("Get information concerning global navigation for the current user.")
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource("example-global.json"))
      .setSince("5.2");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    JsonWriter json = response.newJsonWriter().beginObject();
    writePages(json);
    writeLogoProperties(json);
    writeQualifiers(json);
    json.endObject().close();
  }

  private void writePages(JsonWriter json) {
    json.name("globalPages").beginArray();
    for (ViewProxy<Page> page : views.getPages(NavigationSection.HOME)) {
      if (page.isUserAuthorized()) {
        json.beginObject()
          .prop("name", page.getTitle())
          .prop("url", page.isController() ? page.getId() : String.format("/plugins/home/%s", page.getId()))
          .endObject();
      }
    }
    json.endArray();
  }

  private void writeLogoProperties(JsonWriter json) {
    json.prop("logoUrl", settings.getString(SONAR_LF_LOGO_URL));
    json.prop("logoWidth", settings.getString(SONAR_LF_LOGO_WIDTH_PX));
  }

  private void writeQualifiers(JsonWriter json) {
    json.name("qualifiers").beginArray();
    for (ResourceType rootType : resourceTypes.getRoots()) {
      json.value(rootType.getQualifier());
    }
    json.endArray();
  }
}
