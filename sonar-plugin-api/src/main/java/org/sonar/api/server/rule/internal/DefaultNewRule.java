/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.server.rule.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleScope;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RuleTagFormat;
import org.sonar.api.server.rule.RulesDefinition;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.sonar.api.utils.Preconditions.checkArgument;
import static org.sonar.api.utils.Preconditions.checkState;

class DefaultNewRule extends RulesDefinition.NewRule {
  private final String pluginKey;
  private final String repoKey;
  private final String key;
  private RuleType type;
  private String name;
  private String htmlDescription;
  private String markdownDescription;
  private String internalKey;
  private String severity = Severity.MAJOR;
  private boolean template;
  private RuleStatus status = RuleStatus.defaultStatus();
  private DebtRemediationFunction debtRemediationFunction;
  private String gapDescription;
  private final Set<String> tags = new TreeSet<>();
  private final Set<String> securityStandards = new TreeSet<>();
  private final Map<String, RulesDefinition.NewParam> paramsByKey = new HashMap<>();
  private final RulesDefinition.DebtRemediationFunctions functions;
  private boolean activatedByDefault;
  private RuleScope scope;
  private final Set<RuleKey> deprecatedRuleKeys = new TreeSet<>();

  DefaultNewRule(@Nullable String pluginKey, String repoKey, String key) {
    this.pluginKey = pluginKey;
    this.repoKey = repoKey;
    this.key = key;
    this.functions = new DefaultDebtRemediationFunctions(repoKey, key);
  }

  @Override
  public String key() {
    return this.key;
  }

  @CheckForNull
  @Override
  public RuleScope scope() {
    return this.scope;
  }

  @Override
  public DefaultNewRule setScope(RuleScope scope) {
    this.scope = scope;
    return this;
  }

  @Override
  public DefaultNewRule setName(String s) {
    this.name = trimToNull(s);
    return this;
  }

  @Override
  public DefaultNewRule setTemplate(boolean template) {
    this.template = template;
    return this;
  }

  @Override
  public DefaultNewRule setActivatedByDefault(boolean activatedByDefault) {
    this.activatedByDefault = activatedByDefault;
    return this;
  }

  @Override
  public DefaultNewRule setSeverity(String s) {
    checkArgument(Severity.ALL.contains(s), "Severity of rule %s is not correct: %s", this, s);
    this.severity = s;
    return this;
  }

  @Override
  public DefaultNewRule setType(RuleType t) {
    this.type = t;
    return this;
  }

  @Override
  public DefaultNewRule setHtmlDescription(@Nullable String s) {
    checkState(markdownDescription == null, "Rule '%s' already has a Markdown description", this);
    this.htmlDescription = trimToNull(s);
    return this;
  }

  @Override
  public DefaultNewRule setHtmlDescription(@Nullable URL classpathUrl) {
    if (classpathUrl != null) {
      try {
        setHtmlDescription(IOUtils.toString(classpathUrl, UTF_8));
      } catch (IOException e) {
        throw new IllegalStateException("Fail to read: " + classpathUrl, e);
      }
    } else {
      this.htmlDescription = null;
    }
    return this;
  }

  @Override
  public DefaultNewRule setMarkdownDescription(@Nullable String s) {
    checkState(htmlDescription == null, "Rule '%s' already has an HTML description", this);
    this.markdownDescription = trimToNull(s);
    return this;
  }

  @Override
  public DefaultNewRule setMarkdownDescription(@Nullable URL classpathUrl) {
    if (classpathUrl != null) {
      try {
        setMarkdownDescription(IOUtils.toString(classpathUrl, UTF_8));
      } catch (IOException e) {
        throw new IllegalStateException("Fail to read: " + classpathUrl, e);
      }
    } else {
      this.markdownDescription = null;
    }
    return this;
  }

  @Override
  public DefaultNewRule setStatus(RuleStatus status) {
    checkArgument(RuleStatus.REMOVED != status, "Status 'REMOVED' is not accepted on rule '%s'", this);
    this.status = status;
    return this;
  }

  @Override
  public DefaultNewRule setDebtSubCharacteristic(@Nullable String s) {
    return this;
  }

  @Override
  public RulesDefinition.DebtRemediationFunctions debtRemediationFunctions() {
    return functions;
  }

  @Override
  public DefaultNewRule setDebtRemediationFunction(@Nullable DebtRemediationFunction fn) {
    this.debtRemediationFunction = fn;
    return this;
  }

  @Deprecated
  @Override
  public DefaultNewRule setEffortToFixDescription(@Nullable String s) {
    return setGapDescription(s);
  }

  @Override
  public DefaultNewRule setGapDescription(@Nullable String s) {
    this.gapDescription = s;
    return this;
  }

  @Override
  public RulesDefinition.NewParam createParam(String paramKey) {
    checkArgument(!paramsByKey.containsKey(paramKey), "The parameter '%s' is declared several times on the rule %s", paramKey, this);
    DefaultNewParam param = new DefaultNewParam(paramKey);
    paramsByKey.put(paramKey, param);
    return param;
  }

  @CheckForNull
  @Override
  public RulesDefinition.NewParam param(String paramKey) {
    return paramsByKey.get(paramKey);
  }

  @Override
  public Collection<RulesDefinition.NewParam> params() {
    return paramsByKey.values();
  }

  @Override
  public DefaultNewRule addTags(String... list) {
    for (String tag : list) {
      RuleTagFormat.validate(tag);
      tags.add(tag);
    }
    return this;
  }

  @Override
  public DefaultNewRule setTags(String... list) {
    tags.clear();
    addTags(list);
    return this;
  }

  @Override
  public DefaultNewRule addOwaspTop10(RulesDefinition.OwaspTop10... standards) {
    for (RulesDefinition.OwaspTop10 owaspTop10 : standards) {
      String standard = "owaspTop10:" + owaspTop10.name().toLowerCase(Locale.ENGLISH);
      securityStandards.add(standard);
    }
    return this;
  }

  @Override
  public DefaultNewRule addCwe(int... nums) {
    for (int num : nums) {
      String standard = "cwe:" + num;
      securityStandards.add(standard);
    }
    return this;
  }

  @Override
  public DefaultNewRule setInternalKey(@Nullable String s) {
    this.internalKey = s;
    return this;
  }

  void validate() {
    if (isEmpty(name)) {
      throw new IllegalStateException(format("Name of rule %s is empty", this));
    }
    if (isEmpty(htmlDescription) && isEmpty(markdownDescription)) {
      throw new IllegalStateException(format("One of HTML description or Markdown description must be defined for rule %s", this));
    }
  }

  @Override
  public DefaultNewRule addDeprecatedRuleKey(String repository, String key) {
    deprecatedRuleKeys.add(RuleKey.of(repository, key));
    return this;
  }

  String pluginKey() {
    return pluginKey;
  }

  String repoKey() {
    return repoKey;
  }

  RuleType type() {
    return type;
  }

  String name() {
    return name;
  }

  String htmlDescription() {
    return htmlDescription;
  }

  String markdownDescription() {
    return markdownDescription;
  }

  @CheckForNull
  String internalKey() {
    return internalKey;
  }

  String severity() {
    return severity;
  }

  boolean template() {
    return template;
  }

  RuleStatus status() {
    return status;
  }

  DebtRemediationFunction debtRemediationFunction() {
    return debtRemediationFunction;
  }

  String gapDescription() {
    return gapDescription;
  }

  Set<String> tags() {
    return tags;
  }

  Set<String> securityStandards() {
    return securityStandards;
  }

  Map<String, RulesDefinition.NewParam> paramsByKey() {
    return paramsByKey;
  }

  boolean activatedByDefault() {
    return activatedByDefault;
  }

  Set<RuleKey> deprecatedRuleKeys() {
    return deprecatedRuleKeys;
  }

  @Override
  public String toString() {
    return format("[repository=%s, key=%s]", repoKey, key);
  }
}
