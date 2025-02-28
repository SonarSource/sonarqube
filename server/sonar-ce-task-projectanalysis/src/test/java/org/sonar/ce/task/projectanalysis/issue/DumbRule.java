/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.core.rule.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;

import static java.util.Objects.requireNonNull;

public class DumbRule implements Rule {
  private String uuid;
  private RuleKey key;
  private String name;
  private String language;
  private RuleStatus status = RuleStatus.READY;
  private RuleType type = RuleType.CODE_SMELL;
  private Set<String> tags = new HashSet<>();
  private DebtRemediationFunction function;
  private String pluginKey;
  private boolean isExternal;
  private boolean isAdHoc;
  private final Set<String> securityStandards = new HashSet<>();
  private final Map<SoftwareQuality, Severity> defaultImpacts = new EnumMap<>(SoftwareQuality.class);
  private CleanCodeAttribute cleanCodeAttribute;
  private String severity;

  public DumbRule(RuleKey key) {
    this.key = key;
    this.uuid = key.rule();
    this.name = "name_" + key;
  }

  @Override
  public String getUuid() {
    return requireNonNull(uuid);
  }

  @Override
  public RuleKey getKey() {
    return requireNonNull(key);
  }

  @Override
  public String getName() {
    return requireNonNull(name);
  }

  @Override
  @CheckForNull
  public String getLanguage() {
    return language;
  }

  @Override
  public RuleStatus getStatus() {
    return requireNonNull(status);
  }

  @Override
  public Set<String> getTags() {
    return requireNonNull(tags);
  }

  @Override
  public RuleType getType() {
    return type;
  }

  @Override
  public DebtRemediationFunction getRemediationFunction() {
    return function;
  }

  @Override
  public String getPluginKey() {
    return pluginKey;
  }

  @Override
  public String getDefaultRuleDescription() {
    return null;
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

  @CheckForNull
  @Override
  public CleanCodeAttribute cleanCodeAttribute() {
    return cleanCodeAttribute;
  }

  @Override
  public boolean isExternal() {
    return isExternal;
  }

  @Override
  public boolean isAdHoc() {
    return isAdHoc;
  }

  public DumbRule setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public DumbRule setName(String name) {
    this.name = name;
    return this;
  }

  public DumbRule setLanguage(@Nullable String language) {
    this.language = language;
    return this;
  }

  public DumbRule setStatus(RuleStatus status) {
    this.status = status;
    return this;
  }

  public DumbRule setFunction(@Nullable DebtRemediationFunction function) {
    this.function = function;
    return this;
  }

  public DumbRule setTags(Set<String> tags) {
    this.tags = tags;
    return this;
  }

  public DumbRule setType(RuleType type) {
    this.type = type;
    return this;
  }

  public DumbRule setSeverity(String severity) {
    this.severity = severity;
    return this;
  }

  public DumbRule setPluginKey(String pluginKey) {
    this.pluginKey = pluginKey;
    return this;
  }

  public DumbRule setIsExternal(boolean isExternal) {
    this.isExternal = isExternal;
    return this;
  }

  public DumbRule setIsAdHoc(boolean isAdHoc) {
    this.isAdHoc = isAdHoc;
    return this;
  }

  public DumbRule addDefaultImpact(SoftwareQuality softwareQuality, Severity severity) {
    defaultImpacts.put(softwareQuality, severity);
    return this;
  }

  public DumbRule setCleanCodeAttribute(CleanCodeAttribute cleanCodeAttribute) {
    this.cleanCodeAttribute = cleanCodeAttribute;
    return this;
  }

}
