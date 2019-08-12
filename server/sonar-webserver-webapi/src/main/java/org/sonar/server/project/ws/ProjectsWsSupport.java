/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.project.ws;

import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.organization.BillingValidations;
import org.sonar.server.organization.BillingValidationsProxy;
import org.sonar.server.organization.DefaultOrganizationProvider;

import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;

@ServerSide
public class ProjectsWsSupport {
  public static final String PARAM_ORGANIZATION = "organization";

  private final DbClient dbClient;
  private final DefaultOrganizationProvider organizationProvider;
  private final BillingValidationsProxy billingValidations;

  public ProjectsWsSupport(DbClient dbClient, DefaultOrganizationProvider organizationProvider, BillingValidationsProxy billingValidations) {
    this.dbClient = dbClient;
    this.organizationProvider = organizationProvider;
    this.billingValidations = billingValidations;
  }

  void addOrganizationParam(WebService.NewAction action) {
    action.createParam(PARAM_ORGANIZATION)
      .setDescription("The key of the organization")
      .setRequired(false)
      .setInternal(true)
      .setSince("6.3");
  }

  OrganizationDto getOrganization(DbSession dbSession, @Nullable String organizationKeyParam) {
    String organizationKey = organizationKeyParam == null ? organizationProvider.get().getKey() : organizationKeyParam;
    return checkFoundWithOptional(
      dbClient.organizationDao().selectByKey(dbSession, organizationKey),
      "No organization for key '%s'", organizationKey);
  }

  void checkCanUpdateProjectsVisibility(OrganizationDto organizationDto, boolean newProjectsPrivate) {
    try {
      BillingValidations.Organization organization = new BillingValidations.Organization(organizationDto.getKey(), organizationDto.getUuid(), organizationDto.getName());
      billingValidations.checkCanUpdateProjectVisibility(organization, newProjectsPrivate);
    } catch (BillingValidations.BillingValidationsException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }
}
