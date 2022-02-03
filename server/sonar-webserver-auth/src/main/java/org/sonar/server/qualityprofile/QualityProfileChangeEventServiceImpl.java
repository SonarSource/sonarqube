/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.server.ServerSide;
import org.sonar.core.util.ParamChange;
import org.sonar.core.util.RuleChange;
import org.sonar.core.util.RuleSetChangeEvent;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.ProjectQprofileAssociationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;

@ServerSide
public class QualityProfileChangeEventServiceImpl implements QualityProfileChangeEventService {

  private final DbClient dbClient;
  private final RuleIndex ruleIndex;
  private final RuleActivatorEventsDistributor eventsDistributor;

  public QualityProfileChangeEventServiceImpl(DbClient dbClient, RuleIndex ruleIndex, RuleActivatorEventsDistributor eventsDistributor) {
    this.dbClient = dbClient;
    this.ruleIndex = ruleIndex;
    this.eventsDistributor = eventsDistributor;
  }

  @Override
  public void publishRuleActivationToSonarLintClients(ProjectDto project, Optional<QProfileDto> activatedProfile, Optional<QProfileDto> deactivatedProfile) {
    List<RuleChange> activatedRules = new ArrayList<>();
    List<RuleChange> deactivatedRules = new ArrayList<>();

    try (DbSession dbSession = dbClient.openSession(false)) {

      if (activatedProfile.isPresent()) {
        RuleQuery query = new RuleQuery().setQProfile(activatedProfile.get()).setActivation(true).setIncludeExternal(true);
        // .setLanguages() ?
        Iterator<String> searchIdResult = ruleIndex.searchAll(query);
        List<String> uuids = new ArrayList<>();
        while (searchIdResult.hasNext()) {
          uuids.add(searchIdResult.next());
        }

        List<RuleDto> ruleDtos = dbClient.ruleDao().selectByUuids(dbSession, uuids);
        Map<String, List<ActiveRuleParamDto>> paramsByRuleUuid = dbClient.activeRuleDao().selectParamsByActiveRuleUuids(dbSession, uuids)
          .stream().collect(Collectors.groupingBy(ActiveRuleParamDto::getActiveRuleUuid));

        for (RuleDto ruleDto : ruleDtos) {
          RuleChange ruleChange = toRuleChange(ruleDto, paramsByRuleUuid);
          activatedRules.add(ruleChange);
        }
      }

      if (deactivatedProfile.isPresent()) {
        RuleQuery query = new RuleQuery().setQProfile(deactivatedProfile.get()).setActivation(true).setIncludeExternal(true);
        // .setLanguages() ?
        Iterator<String> searchIdResult = ruleIndex.searchAll(query);
        List<String> uuids = new ArrayList<>();
        while (searchIdResult.hasNext()) {
          uuids.add(searchIdResult.next());
        }

        List<RuleDto> ruleDtos = dbClient.ruleDao().selectByUuids(dbSession, uuids);
        Map<String, List<ActiveRuleParamDto>> paramsByRuleUuid = dbClient.activeRuleDao().selectParamsByActiveRuleUuids(dbSession, uuids)
          .stream().collect(Collectors.groupingBy(ActiveRuleParamDto::getActiveRuleUuid));

        for (RuleDto ruleDto : ruleDtos) {
          RuleChange ruleChange = toRuleChange(ruleDto, paramsByRuleUuid);
          deactivatedRules.add(ruleChange);
        }
      }

    }

    RuleSetChangeEvent event = new RuleSetChangeEvent(new String[]{project.getKey()}, activatedRules.toArray(new RuleChange[0]), deactivatedRules.toArray(new RuleChange[0]));
    eventsDistributor.pushEvent(event);
  }

  @NotNull
  private RuleChange toRuleChange(RuleDto ruleDto, Map<String, List<ActiveRuleParamDto>> paramsByRuleUuid) {
    RuleChange ruleChange = new RuleChange();
    ruleChange.setKey(ruleDto.getRuleKey());
    ruleChange.setLanguage(ruleDto.getLanguage());
    ruleChange.setSeverity(ruleDto.getSeverityString());

    List<ParamChange> paramChanges = new ArrayList<>();
    List<ActiveRuleParamDto> activeRuleParamDtos = paramsByRuleUuid.getOrDefault(ruleDto.getUuid(), new ArrayList<>());
    for (ActiveRuleParamDto activeRuleParam : activeRuleParamDtos) {
      paramChanges.add(new ParamChange(activeRuleParam.getKey(), activeRuleParam.getValue()));
    }
    ruleChange.setParams(paramChanges.toArray(new ParamChange[0]));

    String templateUuid = ruleDto.getTemplateUuid();
    if (templateUuid != null && !"".equals(templateUuid)) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        RuleDto templateRule = dbClient.ruleDao().selectByUuid(templateUuid, dbSession)
          .orElseThrow(() -> new IllegalStateException(String.format("unknow Template Rule '%s'", templateUuid)));
        ruleChange.setTemplateKey(templateRule.getRuleKey());
      }
    }

    return ruleChange;
  }

  public void distributeRuleChangeEvent(Collection<QProfileDto> profiles, List<ActiveRuleChange> activeRuleChanges, String language) {
    if (activeRuleChanges.isEmpty()) {
      return;
    }

    Set<RuleChange> activatedRules = new HashSet<>();
    Set<RuleChange> deactivatedRules = new HashSet<>();

    for (ActiveRuleChange arc : activeRuleChanges) {

      RuleChange ruleChange = new RuleChange();
      ruleChange.setKey(arc.getActiveRule().getRuleKey().rule());
      ruleChange.setSeverity(arc.getSeverity());
      ruleChange.setLanguage(language);

      Optional<String> templateKey = templateKey(arc);
      templateKey.ifPresent(ruleChange::setTemplateKey);

      // params
      List<ParamChange> paramChanges = new ArrayList<>();
      for (Map.Entry<String, String> entry : arc.getParameters().entrySet()) {
        paramChanges.add(new ParamChange(entry.getKey(), entry.getValue()));
      }
      ruleChange.setParams(paramChanges.toArray(new ParamChange[0]));

      switch (arc.getType()) {
        case ACTIVATED:
        case UPDATED:
          activatedRules.add(ruleChange);
          break;
        case DEACTIVATED:
          deactivatedRules.add(ruleChange);
          break;
      }
    }

    Set<String> projectKeys = getProjectKeys(profiles);

    RuleSetChangeEvent event = new RuleSetChangeEvent(projectKeys.toArray(new String[0]), activatedRules.toArray(new RuleChange[0]), deactivatedRules.toArray(new RuleChange[0]));
    eventsDistributor.pushEvent(event);

  }

  private Optional<String> templateKey(ActiveRuleChange arc) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String ruleUuid = arc.getRuleUuid();
      RuleDto rule = dbClient.ruleDao().selectByUuid(ruleUuid, dbSession).orElseThrow(() -> new IllegalStateException("unknow rule"));
      String templateUuid = rule.getTemplateUuid();
      if (templateUuid != null && !"".equals(templateUuid)) {
        RuleDto templateRule = dbClient.ruleDao().selectByUuid(templateUuid, dbSession)
          .orElseThrow(() -> new IllegalStateException(String.format("unknow Template Rule '%s'", templateUuid)));
        return Optional.of(templateRule.getRuleKey());
      }
    }
    return Optional.empty();
  }

  private Set<String> getProjectKeys(Collection<QProfileDto> profiles) {
    Set<String> projectKeys = new HashSet<>();
    try (DbSession dbSession = dbClient.openSession(false)) {
      for (QProfileDto profileDto : profiles) {
        List<ProjectQprofileAssociationDto> associationDtos = dbClient.qualityProfileDao().selectSelectedProjects(dbSession, profileDto, null);
        for (ProjectQprofileAssociationDto associationDto : associationDtos) {
          projectKeys.add(associationDto.getProjectKey());
        }
      }
      return projectKeys;
    }
  }

}
