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
package org.sonar.server.permission.ws.template;

import java.util.Date;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsPermissions.PermissionTemplate;
import org.sonarqube.ws.WsPermissions.UpdateTemplateWsResponse;
import org.sonarqube.ws.client.permission.UpdateTemplateWsRequest;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.lang.String.format;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkProjectAdmin;
import static org.sonar.server.permission.ws.PermissionRequestValidator.MSG_TEMPLATE_WITH_SAME_NAME;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateProjectPattern;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateTemplateNameFormat;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createIdParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createTemplateDescriptionParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createTemplateProjectKeyPatternParameter;
import static org.sonar.server.permission.ws.template.PermissionTemplateDtoToPermissionTemplateResponse.toPermissionTemplateResponse;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_DESCRIPTION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY_PATTERN;

public class UpdateTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final System2 system;
  private final PermissionWsSupport wsSupport;

  public UpdateTemplateAction(DbClient dbClient, UserSession userSession, System2 system, PermissionWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.system = system;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("update_template")
      .setDescription("Update a permission template.<br />" +
        "It requires administration permissions to access.")
      .setResponseExample(getClass().getResource("update_template-example.json"))
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    createIdParameter(action);

    action.createParam(PARAM_NAME)
      .setDescription("Name")
      .setExampleValue("Financial Service Permissions");

    createTemplateProjectKeyPatternParameter(action);
    createTemplateDescriptionParameter(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    UpdateTemplateWsResponse updateTemplateWsResponse = doHandle(toUpdateTemplateWsRequest(request));
    writeProtobuf(updateTemplateWsResponse, request, response);
  }

  private UpdateTemplateWsResponse doHandle(UpdateTemplateWsRequest request) {
    String uuid = request.getId();
    String nameParam = request.getName();
    String descriptionParam = request.getDescription();
    String projectPatternParam = request.getProjectKeyPattern();

    try (DbSession dbSession = dbClient.openSession(false)) {
      PermissionTemplateDto templateToUpdate = getAndBuildTemplateToUpdate(dbSession, uuid, nameParam, descriptionParam, projectPatternParam);
      checkProjectAdmin(userSession, templateToUpdate.getOrganizationUuid(), Optional.empty());

      validateTemplate(dbSession, templateToUpdate);
      PermissionTemplateDto updatedTemplate = updateTemplate(dbSession, templateToUpdate);
      dbSession.commit();

      return buildResponse(updatedTemplate);
    }
  }

  private static UpdateTemplateWsRequest toUpdateTemplateWsRequest(Request request) {
    return new UpdateTemplateWsRequest()
      .setId(request.mandatoryParam(PARAM_ID))
      .setName(request.param(PARAM_NAME))
      .setDescription(request.param(PARAM_DESCRIPTION))
      .setProjectKeyPattern(request.param(PARAM_PROJECT_KEY_PATTERN));
  }

  private void validateTemplate(DbSession dbSession, PermissionTemplateDto templateToUpdate) {
    validateTemplateNameForUpdate(dbSession, templateToUpdate.getOrganizationUuid(), templateToUpdate.getName(), templateToUpdate.getId());
    validateProjectPattern(templateToUpdate.getKeyPattern());
  }

  private PermissionTemplateDto getAndBuildTemplateToUpdate(DbSession dbSession, String uuid, @Nullable String newName, @Nullable String newDescription,
    @Nullable String newProjectKeyPattern) {
    PermissionTemplateDto templateToUpdate = wsSupport.findTemplate(dbSession, WsTemplateRef.newTemplateRef(uuid, null, null));
    templateToUpdate.setName(firstNonNull(newName, templateToUpdate.getName()));
    templateToUpdate.setDescription(firstNonNull(newDescription, templateToUpdate.getDescription()));
    templateToUpdate.setKeyPattern(firstNonNull(newProjectKeyPattern, templateToUpdate.getKeyPattern()));
    templateToUpdate.setUpdatedAt(new Date(system.now()));

    return templateToUpdate;
  }

  private PermissionTemplateDto updateTemplate(DbSession dbSession, PermissionTemplateDto templateToUpdate) {
    return dbClient.permissionTemplateDao().update(dbSession, templateToUpdate);
  }

  private static UpdateTemplateWsResponse buildResponse(PermissionTemplateDto permissionTemplate) {
    PermissionTemplate permissionTemplateBuilder = toPermissionTemplateResponse(permissionTemplate);
    return UpdateTemplateWsResponse.newBuilder().setPermissionTemplate(permissionTemplateBuilder).build();
  }

  private void validateTemplateNameForUpdate(DbSession dbSession, String organizationUuid, String name, long id) {
    validateTemplateNameFormat(name);

    PermissionTemplateDto permissionTemplateWithSameName = dbClient.permissionTemplateDao().selectByName(dbSession, organizationUuid, name);
    checkRequest(permissionTemplateWithSameName == null || permissionTemplateWithSameName.getId() == id,
      format(MSG_TEMPLATE_WITH_SAME_NAME, name));
  }
}
