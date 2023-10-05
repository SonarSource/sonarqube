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
package org.sonar.server.rule.registration;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.RuleImpactChangeDto;
import org.sonar.db.rule.RuleChangeDto;
import org.sonar.server.rule.PluginRuleUpdate;

public class QualityProfileChangesUpdater {

  private final DbClient dbClient;
  private final UuidFactory uuidFactory;

  public QualityProfileChangesUpdater(DbClient dbClient, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
  }

  public void updateWithoutCommit(DbSession dbSession, Set<PluginRuleUpdate> pluginRuleUpdates) {
    for (PluginRuleUpdate pluginRuleUpdate : pluginRuleUpdates) {
      String ruleChangeUuid = uuidFactory.create();
      RuleChangeDto ruleChangeDto = createRuleChange(ruleChangeUuid, pluginRuleUpdate);

      createRuleImpactChanges(ruleChangeUuid, pluginRuleUpdate, ruleChangeDto);
      insertRuleChange(dbSession, ruleChangeDto);

      for (String qualityProfileUuid : findQualityProfilesForRule(dbSession, pluginRuleUpdate.getRuleUuid())) {
        QProfileChangeDto qProfileChangeDto = new QProfileChangeDto();
        qProfileChangeDto.setUuid(uuidFactory.create());
        qProfileChangeDto.setChangeType("UPDATED");
        qProfileChangeDto.setRuleChangeUuid(ruleChangeUuid);
        qProfileChangeDto.setRulesProfileUuid(qualityProfileUuid);
        dbClient.qProfileChangeDao().insert(dbSession, qProfileChangeDto);
      }

    }
  }

  private static RuleChangeDto createRuleChange(String ruleChangeUuid, PluginRuleUpdate pluginRuleUpdate) {
    RuleChangeDto ruleChangeDto = new RuleChangeDto();
    ruleChangeDto.setUuid(ruleChangeUuid);
    ruleChangeDto.setRuleUuid(pluginRuleUpdate.getRuleUuid());
    ruleChangeDto.setOldCleanCodeAttribute(pluginRuleUpdate.getOldCleanCodeAttribute());
    ruleChangeDto.setNewCleanCodeAttribute(pluginRuleUpdate.getNewCleanCodeAttribute());
    return ruleChangeDto;
  }

  private Set<String> findQualityProfilesForRule(DbSession dbSession, String ruleUuid) {
    return dbClient.activeRuleDao().selectByRuleUuid(dbSession, ruleUuid)
      .stream()
      .map(ActiveRuleDto::getProfileUuid)
      .collect(Collectors.toSet());
  }

  private void insertRuleChange(DbSession dbSession, RuleChangeDto ruleChangeDto) {
    dbClient.ruleChangeDao().insert(dbSession, ruleChangeDto);
  }

  private static void createRuleImpactChanges(String ruleChangeUuid, PluginRuleUpdate pluginRuleUpdate, RuleChangeDto ruleChangeDto) {
    List<SoftwareQuality> matchingSoftwareQualities = pluginRuleUpdate.getMatchingSoftwareQualities();
    for (SoftwareQuality softwareQuality : matchingSoftwareQualities) {
      RuleImpactChangeDto ruleImpactChangeDto = new RuleImpactChangeDto();
      ruleImpactChangeDto.setRuleChangeUuid(ruleChangeUuid);
      ruleImpactChangeDto.setOldSeverity(pluginRuleUpdate.getOldImpacts().get(softwareQuality).name());
      ruleImpactChangeDto.setOldSoftwareQuality(softwareQuality.name());
      ruleImpactChangeDto.setNewSeverity(pluginRuleUpdate.getNewImpacts().get(softwareQuality).name());
      ruleImpactChangeDto.setNewSoftwareQuality(softwareQuality.name());
      ruleChangeDto.addRuleImpactChangeDto(ruleImpactChangeDto);
    }

    List<SoftwareQuality> oldSoftwareQualities = pluginRuleUpdate.getOldImpacts().keySet()
      .stream()
      .filter(softwareQuality -> !matchingSoftwareQualities.contains(softwareQuality)).toList();

    List<SoftwareQuality> newSoftwareQualities = pluginRuleUpdate.getNewImpacts().keySet()
      .stream()
      .filter(softwareQuality -> !matchingSoftwareQualities.contains(softwareQuality)).toList();
    
    int size = Math.max(oldSoftwareQualities.size(), newSoftwareQualities.size());
    for(int i = 0; i < size; i++) {
      RuleImpactChangeDto ruleImpactChangeDto = new RuleImpactChangeDto();
      ruleImpactChangeDto.setRuleChangeUuid(ruleChangeUuid);
      if(i < oldSoftwareQualities.size()) {
        ruleImpactChangeDto.setOldSeverity(pluginRuleUpdate.getOldImpacts().get(oldSoftwareQualities.get(i)).name());
        ruleImpactChangeDto.setOldSoftwareQuality(oldSoftwareQualities.get(i).name());
      }
      if(i < newSoftwareQualities.size()) {
        ruleImpactChangeDto.setNewSeverity(pluginRuleUpdate.getNewImpacts().get(newSoftwareQualities.get(i)).name());
        ruleImpactChangeDto.setNewSoftwareQuality(newSoftwareQualities.get(i).name());
      }
      ruleChangeDto.addRuleImpactChangeDto(ruleImpactChangeDto);
    }


  }
}
