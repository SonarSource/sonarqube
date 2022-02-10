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
package org.sonar.server.pushapi.qualityprofile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.server.ServerSide;
import org.sonar.core.util.ParamChange;
import org.sonar.core.util.RuleChange;
import org.sonar.core.util.RuleSetChangeEvent;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.ProjectQprofileAssociationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.qualityprofile.ActiveRuleChange;

@ServerSide
public class QualityProfileChangeEventServiceImpl implements QualityProfileChangeEventService {

  private final DbClient dbClient;
  private final RuleActivatorEventsDistributor eventsDistributor;

  public QualityProfileChangeEventServiceImpl(DbClient dbClient, RuleActivatorEventsDistributor eventsDistributor) {
    this.dbClient = dbClient;
    this.eventsDistributor = eventsDistributor;
  }

  @Override
  public void publishRuleActivationToSonarLintClients(ProjectDto project, @Nullable QProfileDto activatedProfile, @Nullable QProfileDto deactivatedProfile) {
    List<RuleChange> activatedRules = new ArrayList<>();
    List<RuleChange> deactivatedRules = new ArrayList<>();

    if (activatedProfile != null) {
      activatedRules.addAll(createRuleChanges(activatedProfile));
    }

    if (deactivatedProfile != null) {
      deactivatedRules.addAll(createRuleChanges(deactivatedProfile));
    }

    if (activatedRules.isEmpty() && deactivatedRules.isEmpty()) {
      return;
    }

    RuleSetChangeEvent event = new RuleSetChangeEvent(new String[] {project.getKey()}, activatedRules.toArray(new RuleChange[0]), deactivatedRules.toArray(new RuleChange[0]));
    eventsDistributor.pushEvent(event);
  }

  private List<RuleChange> createRuleChanges(@NotNull QProfileDto profileDto) {
    List<RuleChange> ruleChanges = new ArrayList<>();

    try (DbSession dbSession = dbClient.openSession(false)) {
      List<OrgActiveRuleDto> activeRuleDtos = dbClient.activeRuleDao().selectByProfile(dbSession, profileDto);
      List<String> activeRuleUuids = activeRuleDtos.stream().map(ActiveRuleDto::getUuid).collect(Collectors.toList());

      Map<String, List<ActiveRuleParamDto>> paramsByActiveRuleUuid = dbClient.activeRuleDao().selectParamsByActiveRuleUuids(dbSession, activeRuleUuids)
        .stream().collect(Collectors.groupingBy(ActiveRuleParamDto::getActiveRuleUuid));

      Map<String, String> activeRuleUuidByRuleUuid = activeRuleDtos.stream().collect(Collectors.toMap(ActiveRuleDto::getRuleUuid, ActiveRuleDto::getUuid));

      List<String> ruleUuids = activeRuleDtos.stream().map(ActiveRuleDto::getRuleUuid).collect(Collectors.toList());
      List<RuleDto> ruleDtos = dbClient.ruleDao().selectByUuids(dbSession, ruleUuids);

      for (RuleDto ruleDto : ruleDtos) {
        String activeRuleUuid = activeRuleUuidByRuleUuid.get(ruleDto.getUuid());
        List<ActiveRuleParamDto> params = paramsByActiveRuleUuid.getOrDefault(activeRuleUuid, new ArrayList<>());
        RuleChange ruleChange = toRuleChange(ruleDto, params);
        ruleChanges.add(ruleChange);
      }
    }
    return ruleChanges;
  }

  @NotNull
  private RuleChange toRuleChange(RuleDto ruleDto, List<ActiveRuleParamDto> activeRuleParamDtos) {
    RuleChange ruleChange = new RuleChange();
    ruleChange.setKey(ruleDto.getRuleKey());
    ruleChange.setLanguage(ruleDto.getLanguage());
    ruleChange.setSeverity(ruleDto.getSeverityString());

    List<ParamChange> paramChanges = new ArrayList<>();
    for (ActiveRuleParamDto activeRuleParam : activeRuleParamDtos) {
      paramChanges.add(new ParamChange(activeRuleParam.getKey(), activeRuleParam.getValue()));
    }
    ruleChange.setParams(paramChanges.toArray(new ParamChange[0]));

    String templateUuid = ruleDto.getTemplateUuid();
    if (templateUuid != null && !"".equals(templateUuid)) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        RuleDto templateRule = dbClient.ruleDao().selectByUuid(templateUuid, dbSession)
          .orElseThrow(() -> new IllegalStateException(String.format("Unknown Template Rule '%s'", templateUuid)));
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
      if (arc.getActiveRule() == null) {
        continue;
      }

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

    if (activatedRules.isEmpty() && deactivatedRules.isEmpty()) {
      return;
    }

    RuleSetChangeEvent event = new RuleSetChangeEvent(projectKeys.toArray(new String[0]), activatedRules.toArray(new RuleChange[0]), deactivatedRules.toArray(new RuleChange[0]));
    eventsDistributor.pushEvent(event);
  }

  private Optional<String> templateKey(ActiveRuleChange arc) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String ruleUuid = arc.getRuleUuid();
      RuleDto rule = dbClient.ruleDao().selectByUuid(ruleUuid, dbSession).orElseThrow(() -> new IllegalStateException("unknow rule"));
      String templateUuid = rule.getTemplateUuid();

      if (StringUtils.isNotEmpty(templateUuid)) {
        RuleDto templateRule = dbClient.ruleDao().selectByUuid(templateUuid, dbSession)
          .orElseThrow(() -> new IllegalStateException(String.format("Unknown Template Rule '%s'", templateUuid)));
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
