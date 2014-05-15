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

package org.sonar.server.component.ws;

import com.google.common.io.Resources;
import org.sonar.api.component.Component;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.resource.ResourceDao;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ComponentAppAction implements RequestHandler {

  private static final String KEY = "key";

  private final ResourceDao resourceDao;

  public ComponentAppAction(ResourceDao resourceDao) {
    this.resourceDao = resourceDao;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("app")
      .setDescription("Coverage data required for rendering the component viewer")
      .setSince("4.4")
      .setInternal(true)
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "components-app-example-show.json"));

    action
      .createParam(KEY)
      .setRequired(true)
      .setDescription("File key")
      .setExampleValue("my_project:/src/foo/Bar.php");
  }

  @Override
  public void handle(Request request, Response response) {
    String fileKey = request.mandatoryParam(KEY);
    UserSession.get().checkComponentPermission(UserRole.CODEVIEWER, fileKey);

    JsonWriter json = response.newJsonWriter();
    json.beginObject();

    ComponentDto component = resourceDao.selectComponentByKey(fileKey);
    if (component == null) {
      throw new NotFoundException(String.format("Component '%s' does not exists.", fileKey));
    }

    json.prop("key", component.key());
    json.prop("path", component.path());
    json.prop("name", component.name());
    json.prop("q", component.qualifier());

    Component subProject = componentById(component.subProjectId());
    json.prop("subProjectName", subProject != null ? subProject.longName() : null);

    Component project = componentById(component.projectId());
    json.prop("projectName", project != null ? project.longName() : null);

    json.endObject();
    json.close();
  }

  @CheckForNull
  private Component componentById(@Nullable Long componentId) {
    if (componentId != null) {
      return resourceDao.findById(componentId);
    }
    return null;
  }

}
