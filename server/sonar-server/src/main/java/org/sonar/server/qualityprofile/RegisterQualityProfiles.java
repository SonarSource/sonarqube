/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualityprofile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;

import static org.sonar.db.Pagination.forPage;

/**
 * Synchronize Quality profiles during server startup
 */
@ServerSide
public class RegisterQualityProfiles {

  private static final Logger LOGGER = Loggers.get(RegisterQualityProfiles.class);
  private static final Pagination PROCESSED_ORGANIZATIONS_BATCH_SIZE = forPage(1).andSize(2000);

  private final BuiltInQProfileRepository builtInQProfileRepository;
  private final DbClient dbClient;
  private final BuiltInQProfileCreation builtInQProfileCreation;
  private final ActiveRuleIndexer activeRuleIndexer;

  public RegisterQualityProfiles(BuiltInQProfileRepository builtInQProfileRepository,
    DbClient dbClient, BuiltInQProfileCreation builtInQProfileCreation, ActiveRuleIndexer activeRuleIndexer) {
    this.builtInQProfileRepository = builtInQProfileRepository;
    this.dbClient = dbClient;
    this.builtInQProfileCreation = builtInQProfileCreation;
    this.activeRuleIndexer = activeRuleIndexer;
  }

  public void start() {
    Profiler profiler = Profiler.create(Loggers.get(getClass())).startInfo("Register quality profiles");

    try (DbSession session = dbClient.openSession(false)) {
      List<ActiveRuleChange> changes = new ArrayList<>();
      builtInQProfileRepository.getQProfilesByLanguage().forEach(
        (key, value) -> registerPerLanguage(session, value, changes));
      activeRuleIndexer.index(changes);
      profiler.stopDebug();
    }
  }

  private void registerPerLanguage(DbSession session, List<BuiltInQProfile> qualityProfiles, List<ActiveRuleChange> changes) {
    qualityProfiles.forEach(qp -> registerPerQualityProfile(session, qp, changes));
    session.commit();
  }

  private void registerPerQualityProfile(DbSession dbSession, BuiltInQProfile qualityProfile, List<ActiveRuleChange> changes) {
    LOGGER.info("Register profile {}", qualityProfile.getQProfileName());

    renameOutdatedProfiles(dbSession, qualityProfile);

    List<OrganizationDto> organizationDtos;
    while (!(organizationDtos = getOrganizationsWithoutQP(dbSession, qualityProfile)).isEmpty()) {
      organizationDtos.forEach(organization -> registerPerQualityProfileAndOrganization(dbSession, qualityProfile, organization, changes));
    }
  }

  /**
   * The Quality profiles created by users should be renamed when they have the same name
   * as the built-in profile to be persisted.
   *
   * When upgrading from < 6.5 , all existing profiles are considered as "custom" (created
   * by users) because the concept of built-in profile is not persisted. The "Sonar way" profiles
   * are renamed to "Sonar way (outdated copy) in order to avoid conflicts with the new
   * built-in profile "Sonar way", which has probably different configuration.
   */
  private void renameOutdatedProfiles(DbSession dbSession, BuiltInQProfile profile) {
    Collection<String> profileKeys = dbClient.qualityProfileDao().selectOutdatedProfiles(dbSession, profile.getLanguage(), profile.getName());
    if (profileKeys.isEmpty()) {
      return;
    }
    String newName = profile.getName() + " (outdated copy)";
    LOGGER.info("Rename Quality profiles [{}/{}] to [{}] in {} organizations", profile.getLanguage(), profile.getName(), newName, profileKeys.size());
    dbClient.qualityProfileDao().renameAndCommit(dbSession, profileKeys, newName);
  }

  private List<OrganizationDto> getOrganizationsWithoutQP(DbSession session, BuiltInQProfile qualityProfile) {
    return dbClient.organizationDao().selectOrganizationsWithoutLoadedTemplate(session,
      qualityProfile.getLoadedTemplateType(), PROCESSED_ORGANIZATIONS_BATCH_SIZE);
  }

  private void registerPerQualityProfileAndOrganization(DbSession session, BuiltInQProfile qualityProfile, OrganizationDto organization, List<ActiveRuleChange> changes) {
    LOGGER.debug("Register profile {} for organization {}", qualityProfile.getQProfileName(), organization.getKey());

    builtInQProfileCreation.create(session, qualityProfile, organization, changes);
    session.commit();
  }

}
