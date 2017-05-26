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

import java.util.List;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.organization.OrganizationDto;

import static java.lang.String.format;
import static org.sonar.db.Pagination.forPage;

/**
 * When a property "sonar.qp.massInserts" contains true, this will perform the synchronization of Quality Profiles
 * in DB for every organization with the Quality Profiles defined by plugins before and in place of
 * {@link RegisterQualityProfiles}.
 *
 * This implementation is more efficient than the one of {@link RegisterQualityProfiles} but has a strong limitation:
 * <strong>hierarchies of quality profiles are not supported and an exception will be raised if any child quality profiles
 * is encountered</strong>
 */
@ServerSide
public class MassRegisterQualityProfiles {

  private static final Logger LOGGER = Loggers.get(MassRegisterQualityProfiles.class);
  private static final Pagination PROCESSED_ORGANIZATIONS_BATCH_SIZE = forPage(1).andSize(2000);

  private final Settings settings;
  private final DefinedQProfileRepository definedQProfileRepository;
  private final DbClient dbClient;
  private final DefinedQProfileInsert definedQProfileInsert;

  public MassRegisterQualityProfiles(Settings settings, DefinedQProfileRepository definedQProfileRepository,
    DbClient dbClient, DefinedQProfileInsert definedQProfileInsert) {
    this.settings = settings;
    this.definedQProfileRepository = definedQProfileRepository;
    this.dbClient = dbClient;
    this.definedQProfileInsert = definedQProfileInsert;
  }

  public void start() {
    if (!settings.getBoolean("sonar.qp.massInserts")) {
      return;
    }

    Profiler profiler = Profiler.create(Loggers.get(getClass())).startInfo("Mass Register quality profiles");
    if (definedQProfileRepository.getQProfilesByLanguage().isEmpty()) {
      return;
    }

    try (DbSession session = dbClient.openSession(false);
      DbSession batchSession = dbClient.openSession(true)) {
      definedQProfileRepository.getQProfilesByLanguage()
        .forEach((key, value) -> registerPerLanguage(session, batchSession, value));
      profiler.stopDebug();
    }
  }

  private void registerPerLanguage(DbSession session, DbSession batchSession, List<DefinedQProfile> qualityProfiles) {
    qualityProfiles.forEach(qp -> registerPerQualityProfile(session, batchSession, qp));
  }

  private void registerPerQualityProfile(DbSession session, DbSession batchSession, DefinedQProfile qualityProfile) {
    LOGGER.info("Register profile {}", qualityProfile.getQProfileName());

    Profiler profiler = Profiler.create(Loggers.get(getClass()));
    List<OrganizationDto> organizationDtos;
    while (!(organizationDtos = getOrganizationsWithoutQP(session, qualityProfile)).isEmpty()) {
      organizationDtos.forEach(organization -> registerPerQualityProfileAndOrganization(session, batchSession, qualityProfile, organization, profiler));
    }
  }

  private List<OrganizationDto> getOrganizationsWithoutQP(DbSession session, DefinedQProfile qualityProfile) {
    return dbClient.organizationDao().selectOrganizationsWithoutLoadedTemplate(session,
      qualityProfile.getLoadedTemplateType(), PROCESSED_ORGANIZATIONS_BATCH_SIZE);
  }

  private void registerPerQualityProfileAndOrganization(DbSession session, DbSession batchSession,
    DefinedQProfile definedQProfile, OrganizationDto organization, Profiler profiler) {
    profiler.start();

    definedQProfileInsert.create(session, batchSession, definedQProfile, organization);

    session.commit();
    batchSession.commit();

    profiler.stopDebug(format("Register profile %s for organization %s", definedQProfile.getQProfileName(), organization.getKey()));
  }

}
