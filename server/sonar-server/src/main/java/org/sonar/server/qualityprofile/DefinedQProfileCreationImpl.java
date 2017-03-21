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
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;

public class DefinedQProfileCreationImpl implements DefinedQProfileCreation {
  private final DbClient dbClient;
  private final QProfileFactory profileFactory;
  private final RuleActivator ruleActivator;

  public DefinedQProfileCreationImpl(DbClient dbClient, QProfileFactory profileFactory, RuleActivator ruleActivator) {
    this.dbClient = dbClient;
    this.profileFactory = profileFactory;
    this.ruleActivator = ruleActivator;
  }

  @Override
  public void create(DbSession session, DefinedQProfile qualityProfile, OrganizationDto organization, List<ActiveRuleChange> changes) {
    QualityProfileDto profileDto = dbClient.qualityProfileDao().selectByNameAndLanguage(organization, qualityProfile.getName(), qualityProfile.getLanguage(), session);
    if (profileDto == null) {
      profileDto = profileFactory.create(session, organization, qualityProfile.getQProfileName(), qualityProfile.isDefault());
      for (org.sonar.api.rules.ActiveRule activeRule : qualityProfile.getActiveRules()) {
        RuleKey ruleKey = RuleKey.of(activeRule.getRepositoryKey(), activeRule.getRuleKey());
        RuleActivation activation = new RuleActivation(ruleKey);
        activation.setSeverity(activeRule.getSeverity() != null ? activeRule.getSeverity().name() : null);
        for (ActiveRuleParam param : activeRule.getActiveRuleParams()) {
          activation.setParameter(param.getKey(), param.getValue());
        }
        changes.addAll(ruleActivator.activate(session, activation, profileDto));
      }
    }

    LoadedTemplateDto template = new LoadedTemplateDto(organization.getUuid(), qualityProfile.getLoadedTemplateType());
    dbClient.loadedTemplateDao().insert(template, session);
  }
}
