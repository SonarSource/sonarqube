/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.rule.RuleChangeDto;
import org.sonar.db.rule.RuleImpactChangeDto;
import org.sonar.server.rule.PluginRuleUpdate;

public class QualityProfileChangesUpdater {

  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final SonarQubeVersion sonarQubeVersion;

  public QualityProfileChangesUpdater(DbClient dbClient, UuidFactory uuidFactory, SonarQubeVersion sonarQubeVersion) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.sonarQubeVersion = sonarQubeVersion;
  }

  public void createQprofileChangesForRuleUpdates(DbSession dbSession, Set<PluginRuleUpdate> pluginRuleUpdates) {
    List<QProfileChangeDto> changesToPersist = pluginRuleUpdates.stream()
      .flatMap(pluginRuleUpdate -> {
        RuleChangeDto ruleChangeDto = createNewRuleChange(pluginRuleUpdate);
        insertRuleChange(dbSession, ruleChangeDto);

        return findQualityProfilesForRule(dbSession, pluginRuleUpdate.getRuleUuid()).stream()
          .map(qualityProfileUuid -> buildQprofileChangeDtoForRuleChange(qualityProfileUuid, ruleChangeDto));
      }).toList();

    if (!changesToPersist.isEmpty()) {
      dbClient.qProfileChangeDao().bulkInsert(dbSession, changesToPersist);
    }
  }

  private RuleChangeDto createNewRuleChange(PluginRuleUpdate pluginRuleUpdate) {
    RuleChangeDto ruleChangeDto = new RuleChangeDto();
    ruleChangeDto.setUuid(uuidFactory.create());
    ruleChangeDto.setRuleUuid(pluginRuleUpdate.getRuleUuid());
    ruleChangeDto.setOldCleanCodeAttribute(pluginRuleUpdate.getOldCleanCodeAttribute());
    ruleChangeDto.setNewCleanCodeAttribute(pluginRuleUpdate.getNewCleanCodeAttribute());

    ruleChangeDto.setRuleImpactChanges(createRuleImpactChanges(pluginRuleUpdate, ruleChangeDto));
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

  private static Set<RuleImpactChangeDto> createRuleImpactChanges(PluginRuleUpdate pluginRuleUpdate, RuleChangeDto ruleChangeDto) {
    Set<RuleImpactChangeDto> ruleImpactChangeDtos = new HashSet<>();

    pluginRuleUpdate.getMatchingSoftwareQualities().stream()
      .map(softwareQuality -> {
        RuleImpactChangeDto ruleImpactChangeDto = new RuleImpactChangeDto();
        ruleImpactChangeDto.setRuleChangeUuid(ruleChangeDto.getUuid());
        ruleImpactChangeDto.setOldSeverity(pluginRuleUpdate.getOldImpacts().get(softwareQuality));
        ruleImpactChangeDto.setOldSoftwareQuality(softwareQuality);
        ruleImpactChangeDto.setNewSeverity(pluginRuleUpdate.getNewImpacts().get(softwareQuality));
        ruleImpactChangeDto.setNewSoftwareQuality(softwareQuality);
        return ruleImpactChangeDto;
      }).forEach(ruleImpactChangeDtos::add);

    Iterator<SoftwareQuality> removedIterator = (Sets.difference(pluginRuleUpdate.getOldImpacts().keySet(), pluginRuleUpdate.getMatchingSoftwareQualities())).iterator();
    Iterator<SoftwareQuality> addedIterator = (Sets.difference(pluginRuleUpdate.getNewImpacts().keySet(), pluginRuleUpdate.getMatchingSoftwareQualities())).iterator();
    while (removedIterator.hasNext() || addedIterator.hasNext()) {
      RuleImpactChangeDto ruleImpactChangeDto = new RuleImpactChangeDto();
      ruleImpactChangeDto.setRuleChangeUuid(ruleChangeDto.getUuid());
      if (removedIterator.hasNext()) {
        var removedSoftwareQuality = removedIterator.next();
        ruleImpactChangeDto.setOldSoftwareQuality(removedSoftwareQuality);
        ruleImpactChangeDto.setOldSeverity(pluginRuleUpdate.getOldImpacts().get(removedSoftwareQuality));
      }
      if (addedIterator.hasNext()) {
        var addedSoftwareQuality = addedIterator.next();
        ruleImpactChangeDto.setNewSoftwareQuality(addedSoftwareQuality);
        ruleImpactChangeDto.setNewSeverity(pluginRuleUpdate.getNewImpacts().get(addedSoftwareQuality));
      }
      ruleImpactChangeDtos.add(ruleImpactChangeDto);
    }

    return ruleImpactChangeDtos;
  }

  @NotNull
  private QProfileChangeDto buildQprofileChangeDtoForRuleChange(String qualityProfileUuid, RuleChangeDto ruleChangeDto) {
    QProfileChangeDto qProfileChangeDto = new QProfileChangeDto();
    qProfileChangeDto.setUuid(uuidFactory.create());
    qProfileChangeDto.setChangeType("UPDATED");
    qProfileChangeDto.setRuleChange(ruleChangeDto);
    qProfileChangeDto.setRulesProfileUuid(qualityProfileUuid);
    qProfileChangeDto.setSqVersion(sonarQubeVersion.toString());
    return qProfileChangeDto;
  }
}
