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
package org.sonar.server.organization.ws;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationQuery;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.organization.BillingValidations;
import org.sonar.server.organization.BillingValidationsProxy;
import org.sonar.server.project.Project;
import org.sonar.server.project.ProjectLifeCycleListeners;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.user.index.UserIndexer;

import static org.sonar.db.Pagination.forPage;

public class OrganizationDeleter {

  private static final Logger LOGGER = Loggers.get(OrganizationDeleter.class);

  @VisibleForTesting
  static final int PAGE_SIZE = 100;

  private final DbClient dbClient;
  private final ComponentCleanerService componentCleanerService;
  private final UserIndexer userIndexer;
  private final QProfileFactory qProfileFactory;
  private final ProjectLifeCycleListeners projectLifeCycleListeners;
  private final BillingValidationsProxy billingValidations;

  public OrganizationDeleter(DbClient dbClient, ComponentCleanerService componentCleanerService, UserIndexer userIndexer,
    QProfileFactory qProfileFactory, ProjectLifeCycleListeners projectLifeCycleListeners,
    BillingValidationsProxy billingValidations) {
    this.dbClient = dbClient;
    this.componentCleanerService = componentCleanerService;
    this.userIndexer = userIndexer;
    this.qProfileFactory = qProfileFactory;
    this.projectLifeCycleListeners = projectLifeCycleListeners;
    this.billingValidations = billingValidations;
  }

  void delete(DbSession dbSession, OrganizationDto organization) {
    deleteProjects(dbSession, organization);
    deletePermissions(dbSession, organization);
    deleteGroups(dbSession, organization);
    deleteQualityProfiles(dbSession, organization);
    deleteQualityGates(dbSession, organization);
    deleteOrganizationAlmBinding(dbSession, organization);
    deleteOrganization(dbSession, organization);
    billingValidations.onDelete(new BillingValidations.Organization(organization.getKey(), organization.getUuid(), organization.getName()));
  }

  private void deleteProjects(DbSession dbSession, OrganizationDto organization) {
    List<ComponentDto> roots = dbClient.componentDao().selectProjectsByOrganization(dbSession, organization.getUuid());
    try {
      componentCleanerService.delete(dbSession, roots);
    } finally {
      Set<Project> projects = roots.stream()
        .filter(OrganizationDeleter::isMainProject)
        .map(Project::from)
        .collect(MoreCollectors.toSet());
      projectLifeCycleListeners.onProjectsDeleted(projects);
    }
  }

  private static boolean isMainProject(ComponentDto p) {
    return Scopes.PROJECT.equals(p.scope())
      && Qualifiers.PROJECT.equals(p.qualifier())
      && p.getMainBranchProjectUuid() == null;
  }

  private void deletePermissions(DbSession dbSession, OrganizationDto organization) {
    dbClient.permissionTemplateDao().deleteByOrganization(dbSession, organization.getUuid());
    dbClient.userPermissionDao().deleteByOrganization(dbSession, organization.getUuid());
    dbClient.groupPermissionDao().deleteByOrganization(dbSession, organization.getUuid());
  }

  private void deleteGroups(DbSession dbSession, OrganizationDto organization) {
    dbClient.groupDao().deleteByOrganization(dbSession, organization.getUuid());
  }

  private void deleteQualityProfiles(DbSession dbSession, OrganizationDto organization) {
    List<QProfileDto> profiles = dbClient.qualityProfileDao().selectOrderedByOrganizationUuid(dbSession, organization);
    qProfileFactory.delete(dbSession, profiles);
  }

  private void deleteQualityGates(DbSession dbSession, OrganizationDto organization) {
    Collection<QualityGateDto> qualityGates = dbClient.qualityGateDao().selectAll(dbSession, organization);
    dbClient.qualityGateDao().deleteByUuids(dbSession, qualityGates.stream()
      .filter(q -> !q.isBuiltIn())
      .map(QualityGateDto::getUuid)
      .collect(MoreCollectors.toList()));
    dbClient.qualityGateDao().deleteOrgQualityGatesByOrganization(dbSession, organization);
  }

  private void deleteOrganizationAlmBinding(DbSession dbSession, OrganizationDto organization) {
    dbClient.organizationAlmBindingDao().deleteByOrganization(dbSession, organization);
  }

  private void deleteOrganization(DbSession dbSession, OrganizationDto organization) {
    Collection<String> uuids = dbClient.organizationMemberDao().selectUserUuidsByOrganizationUuid(dbSession, organization.getUuid());
    dbClient.organizationMemberDao().deleteByOrganizationUuid(dbSession, organization.getUuid());
    dbClient.organizationDao().deleteByUuid(dbSession, organization.getUuid());
    dbClient.userDao().cleanHomepage(dbSession, organization);
    dbClient.webhookDao().selectByOrganizationUuid(dbSession, organization.getUuid())
      .forEach(wh -> dbClient.webhookDeliveryDao().deleteByWebhook(dbSession, wh));
    dbClient.webhookDao().deleteByOrganization(dbSession, organization);
    List<UserDto> users = dbClient.userDao().selectByUuids(dbSession, uuids);
    userIndexer.commitAndIndex(dbSession, users);
  }

  void deleteByQuery(OrganizationQuery query) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      deleteByQuery(dbSession, query);
    }
  }

  private void deleteByQuery(DbSession dbSession, OrganizationQuery query) {
    while (true) {
      int total = dbClient.organizationDao().countByQuery(dbSession, query);
      if (total == 0) {
        return;
      }

      dbClient.organizationDao().selectByQuery(dbSession, query, forPage(1).andSize(PAGE_SIZE))
        .forEach(org -> {
          LOGGER.info("deleting organization {} ({})", org.getName(), org.getKey());
          delete(dbSession, org);
        });
    }
  }
}
