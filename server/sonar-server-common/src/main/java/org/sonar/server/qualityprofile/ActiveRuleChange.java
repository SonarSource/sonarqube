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
package org.sonar.server.qualityprofile;

import com.google.common.base.MoreObjects;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.rule.RuleChangeDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleImpactChangeDto;

public class ActiveRuleChange {

  private ActiveRuleDto activeRule;
  private RuleDto rule;

  public enum Type {
    ACTIVATED, DEACTIVATED, UPDATED
  }

  private final Type type;
  private final ActiveRuleKey key;
  private final String ruleUuid;
  private String severity = null;
  private final Map<SoftwareQuality, Severity> oldImpacts = new EnumMap<>(SoftwareQuality.class);
  private final Map<SoftwareQuality, Severity> newImpacts = new EnumMap<>(SoftwareQuality.class);
  private Boolean prioritizedRule = null;
  private ActiveRuleInheritance inheritance = null;
  private final Map<String, String> parameters = new HashMap<>();

  public ActiveRuleChange(Type type, ActiveRuleDto activeRule, RuleDto ruleDto) {
    this.type = type;
    this.key = activeRule.getKey();
    this.ruleUuid = ruleDto.getUuid();
    this.activeRule = activeRule;
  }

  public ActiveRuleChange(Type type, ActiveRuleKey key, RuleDto ruleDto) {
    this.type = type;
    this.key = key;
    this.rule = ruleDto;
    this.ruleUuid = ruleDto.getUuid();
  }

  public ActiveRuleKey getKey() {
    return key;
  }

  public String getRuleUuid() {
    return ruleUuid;
  }

  public Type getType() {
    return type;
  }

  @CheckForNull
  public String getSeverity() {
    return severity;
  }

  public ActiveRuleChange setSeverity(@Nullable String s) {
    this.severity = s;
    return this;
  }

  public Map<SoftwareQuality, Severity> getNewImpacts() {
    return newImpacts;
  }

  public ActiveRuleChange setNewImpacts(Map<SoftwareQuality, Severity> newImpacts) {
    this.newImpacts.clear();
    this.newImpacts.putAll(newImpacts);
    return this;
  }

  public ActiveRuleChange setPrioritizedRule(@Nullable Boolean prioritizedRule) {
    this.prioritizedRule = prioritizedRule;
    return this;
  }

  public ActiveRuleChange setOldImpacts(Map<SoftwareQuality, Severity> impactSeverities) {
    this.oldImpacts.clear();
    this.oldImpacts.putAll(impactSeverities);
    return this;
  }

  public ActiveRuleChange setRule(RuleDto rule) {
    this.rule = rule;
    return this;
  }

  @CheckForNull
  public Boolean isPrioritizedRule() {
    return prioritizedRule;
  }

  public ActiveRuleChange setInheritance(@Nullable ActiveRuleInheritance i) {
    this.inheritance = i;
    return this;
  }

  @CheckForNull
  public ActiveRuleInheritance getInheritance() {
    return inheritance;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public ActiveRuleChange setParameter(String key, @Nullable String value) {
    parameters.put(key, value);
    return this;
  }

  public ActiveRuleChange setParameters(Map<String, String> m) {
    parameters.clear();
    parameters.putAll(m);
    return this;
  }

  @CheckForNull
  public ActiveRuleDto getActiveRule() {
    return activeRule;
  }

  public ActiveRuleChange setActiveRule(@Nullable ActiveRuleDto activeRule) {
    this.activeRule = activeRule;
    return this;
  }

  public QProfileChangeDto toSystemChangedDto() {
    return toDto(null);
  }

  public QProfileChangeDto toDto(@Nullable String userUuid) {
    QProfileChangeDto dto = new QProfileChangeDto();
    dto.setChangeType(type.name());
    dto.setRulesProfileUuid(getKey().getRuleProfileUuid());
    dto.setUserUuid(userUuid);
    RuleChangeDto ruleChange = new RuleChangeDto();
    ruleChange.setRuleImpactChanges(createRuleImpactChanges());
    ruleChange.setRuleUuid(ruleUuid);
    if (!ruleChange.getRuleImpactChanges().isEmpty()) {
      dto.setRuleChange(ruleChange);
    }
    Map<String, String> data = new HashMap<>();
    data.put("ruleUuid", getRuleUuid());

    parameters.entrySet().stream()
      .filter(param -> !param.getKey().isEmpty())
      .forEach(param -> data.put("param_" + param.getKey(), param.getValue()));

    if (StringUtils.isNotEmpty(severity)) {
      data.put("severity", severity);
    }
    if (prioritizedRule != null) {
      data.put("prioritizedRule", prioritizedRule.toString());
    }
    dto.setData(data);
    return dto;
  }

  @NotNull
  private Set<RuleImpactChangeDto> createRuleImpactChanges() {
    // We only compute ruleImpactChanges in case of the update or activate
    if ((type == Type.UPDATED || type == Type.ACTIVATED) && rule != null) {
      MapDifference<SoftwareQuality, Severity> difference = Maps.difference(
        computeEffectiveImpactMap(rule, oldImpacts),
        computeEffectiveImpactMap(rule, newImpacts));

      return difference.entriesDiffering().entrySet().stream()
        .map(e -> new RuleImpactChangeDto(e.getKey(), e.getKey(), e.getValue().rightValue(), e.getValue().leftValue()))
        .collect(Collectors.toSet());
    }
    return Set.of();
  }

  public static Map<SoftwareQuality, Severity> computeEffectiveImpactMap(RuleDto ruleDto, Map<SoftwareQuality, Severity> activeRuleImpacts) {
    Map<SoftwareQuality, Severity> impacts = ruleDto.getDefaultImpactsMap();
    impacts.replaceAll(activeRuleImpacts::getOrDefault);
    return impacts;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("type", type)
      .add("key", key)
      .add("ruleUuid", ruleUuid)
      .add("severity", severity)
      .add("inheritance", inheritance)
      .add("parameters", parameters)
      .add("prioritizedRule", prioritizedRule)
      .add("impactSeverities", newImpacts)
      .toString();
  }
}
