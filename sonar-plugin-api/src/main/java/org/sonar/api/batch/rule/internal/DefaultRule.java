/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.batch.rule.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.batch.debt.DebtRemediationFunction;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.RuleParam;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;

@Immutable
public class DefaultRule implements Rule {

  private final RuleKey key;
  private final Integer id;
  private final String name;
  private final String severity;
  private final String type;
  private final String description;
  private final String internalKey;
  private final RuleStatus status;
  private final Map<String, RuleParam> params;

  DefaultRule(NewRule newRule) {
    this.key = newRule.key;
    this.id = newRule.id;
    this.name = newRule.name;
    this.severity = newRule.severity;
    this.type = newRule.type;
    this.description = newRule.description;
    this.internalKey = newRule.internalKey;
    this.status = newRule.status;

    Map<String, RuleParam> builder = new HashMap<>();
    for (NewRuleParam newRuleParam : newRule.params.values()) {
      builder.put(newRuleParam.key, new DefaultRuleParam(newRuleParam));
    }
    params = Collections.unmodifiableMap(builder);
  }

  @Override
  public RuleKey key() {
    return key;
  }

  @CheckForNull
  public Integer id() {
    return id;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String severity() {
    return severity;
  }

  @CheckForNull
  public String type() {
    return type;
  }

  @Override
  public String description() {
    return description;
  }

  @Override
  public String internalKey() {
    return internalKey;
  }

  @Override
  public RuleStatus status() {
    return status;
  }

  @Override
  public String debtSubCharacteristic() {
    throw new UnsupportedOperationException("Debt characteristic is not available by analyzer since version 5.2 (data computation moved to server)");
  }

  @Override
  public DebtRemediationFunction debtRemediationFunction() {
    throw new UnsupportedOperationException("Debt remediation function is not available by analyzer since version 5.2 (data computation moved to server)");
  }

  @Override
  public RuleParam param(String paramKey) {
    return params.get(paramKey);
  }

  @Override
  public Collection<RuleParam> params() {
    return params.values();
  }
}
