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
import java.util.List;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;

import static org.sonar.db.Pagination.forPage;

/**
 * Synchronize Quality profiles during server startup
 */
@ServerSide
public class RegisterQualityProfiles {

  private static final Logger LOGGER = Loggers.get(RegisterQualityProfiles.class);
  private static final Pagination PROCESSED_ORGANIZATIONS_BATCH_SIZE = forPage(1).andSize(2000);

  private final DefinedQProfileRepository definedQProfileRepository;
  private final DbClient dbClient;
  private final QProfileFactory profileFactory;
  private final RuleActivator ruleActivator;
  private final ActiveRuleIndexer activeRuleIndexer;

  public RegisterQualityProfiles(DefinedQProfileRepository definedQProfileRepository,
    DbClient dbClient,
    QProfileFactory profileFactory, CachingRuleActivator ruleActivator, ActiveRuleIndexer activeRuleIndexer) {
    this.definedQProfileRepository = definedQProfileRepository;
    this.dbClient = dbClient;
    this.profileFactory = profileFactory;
    this.ruleActivator = ruleActivator;
    this.activeRuleIndexer = activeRuleIndexer;
  }

  public void start() {
    Profiler profiler = Profiler.create(Loggers.get(getClass())).startInfo("Register quality profiles");

    definedQProfileRepository.initialize();
    try (DbSession session = dbClient.openSession(false)) {
      List<ActiveRuleChange> changes = new ArrayList<>();
      definedQProfileRepository.getQProfilesByLanguage().entrySet()
        .forEach(entry -> registerPerLanguage(session, entry.getValue(), changes));
      activeRuleIndexer.index(changes);
      profiler.stopDebug();
    }
  }

  private void registerPerLanguage(DbSession session, List<DefinedQProfile> qualityProfiles, List<ActiveRuleChange> changes) {
    qualityProfiles.forEach(qp -> registerPerQualityProfile(session, qp, changes));
    session.commit();
  }

  private void registerPerQualityProfile(DbSession session, DefinedQProfile qualityProfile, List<ActiveRuleChange> changes) {
    LOGGER.info("Register profile {}", qualityProfile.getQProfileName());

    List<OrganizationDto> organizationDtos;
    while (!(organizationDtos = getOrganizationsWithoutQP(session, qualityProfile)).isEmpty()) {
      organizationDtos.forEach(organization -> registerPerQualityProfileAndOrganization(session, qualityProfile, organization, changes));
    }
  }

  private List<OrganizationDto> getOrganizationsWithoutQP(DbSession session, DefinedQProfile qualityProfile) {
    return dbClient.organizationDao().selectOrganizationsWithoutLoadedTemplate(session,
      qualityProfile.getLoadedTemplateType(), PROCESSED_ORGANIZATIONS_BATCH_SIZE);
  }

  private void registerPerQualityProfileAndOrganization(DbSession session, DefinedQProfile qualityProfile, OrganizationDto organization, List<ActiveRuleChange> changes) {
    LOGGER.debug("Register profile {} for organization {}", qualityProfile.getQProfileName(), organization.getKey());

    QualityProfileDto profileDto = dbClient.qualityProfileDao().selectByNameAndLanguage(organization, qualityProfile.getName(), qualityProfile.getLanguage(), session);
    if (profileDto != null) {
      changes.addAll(profileFactory.delete(session, profileDto.getKey(), true));
    }
    QualityProfileDto newQProfileDto = profileFactory.create(session, organization, qualityProfile.getQProfileName(), qualityProfile.isDefault());
    for (org.sonar.api.rules.ActiveRule activeRule : qualityProfile.getActiveRules()) {
      RuleKey ruleKey = RuleKey.of(activeRule.getRepositoryKey(), activeRule.getRuleKey());
      RuleActivation activation = new RuleActivation(ruleKey);
      activation.setSeverity(activeRule.getSeverity() != null ? activeRule.getSeverity().name() : null);
      for (ActiveRuleParam param : activeRule.getActiveRuleParams()) {
        activation.setParameter(param.getKey(), param.getValue());
      }
      changes.addAll(ruleActivator.activate(session, activation, newQProfileDto));
    }

    LoadedTemplateDto template = new LoadedTemplateDto(organization.getUuid(), qualityProfile.getLoadedTemplateType());
    dbClient.loadedTemplateDao().insert(template, session);
    session.commit();
  }

}
