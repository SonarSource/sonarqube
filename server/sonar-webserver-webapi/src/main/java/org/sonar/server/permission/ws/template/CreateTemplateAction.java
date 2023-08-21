/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.RequestValidator;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.permission.ws.WsParameters;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Permissions.CreateTemplateWsResponse;
import org.sonarqube.ws.Permissions.PermissionTemplate;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.RequestValidator.MSG_TEMPLATE_WITH_SAME_NAME;
import static org.sonar.server.permission.ws.template.PermissionTemplateDtoToPermissionTemplateResponse.toPermissionTemplateResponse;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_DESCRIPTION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY_PATTERN;

public class CreateTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final System2 system;
  private final PermissionWsSupport wsSupport;
  private final WsParameters wsParameters;
  private static final Logger logger = Loggers.get(CreateTemplateAction.class);

  public CreateTemplateAction(DbClient dbClient, UserSession userSession, System2 system, PermissionWsSupport wsSupport, WsParameters wsParameters) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.system = system;
    this.wsSupport = wsSupport;
    this.wsParameters = wsParameters;
  }

  private static CreateTemplateRequest toCreateTemplateWsRequest(Request request) {
    return new CreateTemplateRequest()
      .setOrganization(request.param(PARAM_ORGANIZATION))
      .setName(request.mandatoryParam(PARAM_NAME))
      .setDescription(request.param(PARAM_DESCRIPTION))
      .setProjectKeyPattern(request.param(PARAM_PROJECT_KEY_PATTERN));
  }

  private static CreateTemplateWsResponse buildResponse(PermissionTemplateDto permissionTemplateDto) {
    PermissionTemplate permissionTemplateBuilder = toPermissionTemplateResponse(permissionTemplateDto);
    return CreateTemplateWsResponse.newBuilder().setPermissionTemplate(permissionTemplateBuilder).build();
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("create_template")
      .setDescription("Create a permission template.<br />" +
        "Requires the following permission: 'Administer System'.")
      .setResponseExample(getClass().getResource("create_template-example.json"))
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_NAME)
      .setRequired(true)
      .setDescription("Name")
      .setExampleValue("Financial Service Permissions");

    wsParameters.createOrganizationParameter(action);
    WsParameters.createTemplateProjectKeyPatternParameter(action);
    WsParameters.createTemplateDescriptionParameter(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    CreateTemplateWsResponse createTemplateWsResponse = doHandle(toCreateTemplateWsRequest(request));
    writeProtobuf(createTemplateWsResponse, request, response);
  }

  private CreateTemplateWsResponse doHandle(CreateTemplateRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      logger.info("Create Permission Template Request :: organization_key : {}, templateName: {}",
              request.getOrganization(), request.getName());
      OrganizationDto org = wsSupport.findOrganization(dbSession, request.getOrganization());
      checkGlobalAdmin(userSession, org.getUuid());

      validateTemplateNameForCreation(dbSession, org, request.getName());
      RequestValidator.validateProjectPattern(request.getProjectKeyPattern());

      PermissionTemplateDto permissionTemplate = insertTemplate(dbSession, org, request);

      return buildResponse(permissionTemplate);
    }
  }

  private void validateTemplateNameForCreation(DbSession dbSession, OrganizationDto org, String name) {
    logger.debug("Validating Template Name :: organization_key : {}, templateName: {}", org.getKey(), name);
    PermissionTemplateDto permissionTemplateWithSameName = dbClient.permissionTemplateDao()
      .selectByName(dbSession, org.getUuid(), name);
    checkRequest(permissionTemplateWithSameName == null, format(MSG_TEMPLATE_WITH_SAME_NAME, name));
  }

  private PermissionTemplateDto insertTemplate(DbSession dbSession, OrganizationDto org, CreateTemplateRequest request) {
    Date now = new Date(system.now());
    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, new PermissionTemplateDto()
      .setUuid(Uuids.create())
      .setOrganizationUuid(org.getUuid())
      .setName(request.getName())
      .setDescription(request.getDescription())
      .setKeyPattern(request.getProjectKeyPattern())
      .setCreatedAt(now)
      .setUpdatedAt(now));
    dbSession.commit();
    logger.info("Template Creation Completed :: organization_key : {}, templateName: {}", org.getKey(), request.getName());
    return template;
  }

  private static class CreateTemplateRequest {
    private String description;
    private String name;
    private String projectKeyPattern;
    private String organization;

    @CheckForNull
    public String getDescription() {
      return description;
    }

    public CreateTemplateRequest setDescription(@Nullable String description) {
      this.description = description;
      return this;
    }

    public String getName() {
      return name;
    }

    public CreateTemplateRequest setName(String name) {
      this.name = requireNonNull(name);
      return this;
    }

    @CheckForNull
    public String getProjectKeyPattern() {
      return projectKeyPattern;
    }

    public CreateTemplateRequest setProjectKeyPattern(@Nullable String projectKeyPattern) {
      this.projectKeyPattern = projectKeyPattern;
      return this;
    }

    public String getOrganization() {
      return organization;
    }

    public CreateTemplateRequest setOrganization(@Nullable String s) {
      this.organization = s;
      return this;
    }
  }
}
