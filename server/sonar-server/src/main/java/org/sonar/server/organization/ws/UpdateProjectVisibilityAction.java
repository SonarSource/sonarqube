/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.organization.ws;

import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.server.organization.BillingValidations;
import org.sonar.server.organization.BillingValidationsProxy;
import org.sonar.server.project.Visibility;
import org.sonar.server.user.UserSession;

import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_ORGANIZATION;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;

public class UpdateProjectVisibilityAction implements OrganizationsWsAction {
  static final String ACTION = "update_project_visibility";
  static final String PARAM_PROJECT_VISIBILITY = "projectVisibility";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final BillingValidationsProxy billingValidations;

  public UpdateProjectVisibilityAction(UserSession userSession, DbClient dbClient, BillingValidationsProxy billingValidations) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.billingValidations = billingValidations;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setPost(true)
      .setDescription("Update the default visibility for new projects of the specified organization.")
      .setInternal(true)
      .setSince("6.4")
      .setHandler(this);

    action.createParam(PARAM_ORGANIZATION)
      .setRequired(true)
      .setDescription("Organization key")
      .setExampleValue("foo-company");

    action.createParam(PARAM_PROJECT_VISIBILITY)
      .setRequired(true)
      .setDescription("Default visibility for projects")
      .setPossibleValues(Visibility.getLabels());
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String organizationKey = request.mandatoryParam(PARAM_ORGANIZATION);
    boolean newProjectsPrivate = Visibility.isPrivate(request.mandatoryParam(PARAM_PROJECT_VISIBILITY));
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<OrganizationDto> optionalOrganization = dbClient.organizationDao().selectByKey(dbSession, organizationKey);
      OrganizationDto organization = checkFoundWithOptional(optionalOrganization, "No organization with key '" + organizationKey + "' can be found.");
      userSession.checkPermission(OrganizationPermission.ADMINISTER, organization.getUuid());
      checkCanUpdateProjectsVisibility(organization, newProjectsPrivate);
      dbClient.organizationDao().setNewProjectPrivate(dbSession, organization, newProjectsPrivate);
      dbSession.commit();
    }
    response.noContent();
  }

  private void checkCanUpdateProjectsVisibility(OrganizationDto organization, boolean newProjectsPrivate) {
    try {
      billingValidations.checkCanUpdateProjectVisibility(new BillingValidations.Organization(organization.getKey(), organization.getUuid()), newProjectsPrivate);
    } catch (BillingValidations.BillingValidationsException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }
}
