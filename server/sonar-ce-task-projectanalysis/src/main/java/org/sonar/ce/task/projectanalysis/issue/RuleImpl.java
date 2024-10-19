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
package org.sonar.ce.task.projectanalysis.issue;

import com.google.common.base.MoreObjects;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;

import static com.google.common.collect.Sets.union;

@Immutable
public class RuleImpl implements Rule {

  private final String uuid;
  private final RuleKey key;
  private final String name;
  private final String language;
  private final RuleStatus status;
  private final Set<String> tags;
  private final DebtRemediationFunction remediationFunction;
  private final RuleType type;
  private final String pluginKey;
  private final boolean isExternal;
  private final boolean isAdHoc;
  private final String defaultRuleDescription;
  private final String severity;
  private final Set<String> securityStandards;
  private final Map<SoftwareQuality, Severity> defaultImpacts;
  private final CleanCodeAttribute cleanCodeAttribute;

  public RuleImpl(RuleDto dto) {
    this.uuid = dto.getUuid();
    this.key = dto.getKey();
    this.name = dto.getName();
    this.language = dto.getLanguage();
    this.status = dto.getStatus();
    this.tags = union(dto.getSystemTags(), dto.getTags());
    this.remediationFunction = effectiveRemediationFunction(dto);
    this.type = RuleType.valueOfNullable(dto.getType());
    this.pluginKey = dto.getPluginKey();
    this.isExternal = dto.isExternal();
    this.isAdHoc = dto.isAdHoc();
    this.defaultRuleDescription = getNonNullDefaultRuleDescription(dto);
    this.severity = Optional.ofNullable(dto.getSeverityString()).orElse(dto.getAdHocSeverity());
    this.securityStandards = dto.getSecurityStandards();
    this.defaultImpacts = dto.getDefaultImpacts()
      .stream().collect(Collectors.toMap(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity));
    this.cleanCodeAttribute = dto.getCleanCodeAttribute();
  }

  private static String getNonNullDefaultRuleDescription(RuleDto dto) {
    return Optional.ofNullable(dto.getDefaultRuleDescriptionSection())
      .map(RuleDescriptionSectionDto::getContent)
      .orElse("");
  }

  @Override
  public String getUuid() {
    return this.uuid;
  }

  @Override
  public RuleKey getKey() {
    return key;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  @CheckForNull
  public String getLanguage() {
    return language;
  }

  @Override
  public RuleStatus getStatus() {
    return status;
  }

  @Override
  public Set<String> getTags() {
    return tags;
  }

  @Override
  public DebtRemediationFunction getRemediationFunction() {
    return remediationFunction;
  }

  @Override
  public RuleType getType() {
    return type;
  }

  @CheckForNull
  @Override
  public String getPluginKey() {
    return pluginKey;
  }

  public String getDefaultRuleDescription() {
    return defaultRuleDescription;
  }

  @Override
  public String getSeverity() {
    return severity;
  }

  @Override
  public Set<String> getSecurityStandards() {
    return securityStandards;
  }

  @Override
  public Map<SoftwareQuality, Severity> getDefaultImpacts() {
    return defaultImpacts;
  }

  @Override
  public CleanCodeAttribute cleanCodeAttribute() {
    return cleanCodeAttribute;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RuleImpl rule = (RuleImpl) o;
    return key.equals(rule.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("uuid", uuid)
      .add("key", key)
      .add("name", name)
      .add("language", language)
      .add("status", status)
      .add("tags", tags)
      .add("pluginKey", pluginKey)
      .toString();
  }

  @CheckForNull
  private static DebtRemediationFunction effectiveRemediationFunction(RuleDto dto) {
    String fn = dto.getRemediationFunction();
    if (fn != null) {
      return new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.valueOf(fn), dto.getRemediationGapMultiplier(), dto.getRemediationBaseEffort());
    }
    String defaultFn = dto.getDefRemediationFunction();
    if (defaultFn != null) {
      return new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.valueOf(defaultFn), dto.getDefRemediationGapMultiplier(), dto.getDefRemediationBaseEffort());
    }
    return null;
  }

  @Override
  public boolean isAdHoc() {
    return isAdHoc;
  }

  @Override
  public boolean isExternal() {
    return isExternal;
  }
}
