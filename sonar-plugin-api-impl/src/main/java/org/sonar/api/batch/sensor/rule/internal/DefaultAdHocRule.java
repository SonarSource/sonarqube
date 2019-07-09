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
package org.sonar.api.batch.sensor.rule.internal;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.DefaultStorable;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.rule.AdHocRule;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.rules.RuleType;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonar.api.utils.Preconditions.checkState;

public class DefaultAdHocRule extends DefaultStorable implements AdHocRule, NewAdHocRule {
  private Severity severity;
  private RuleType type;
  private String name;
  private String description;
  private String engineId;
  private String ruleId;

  public DefaultAdHocRule() {
    super(null);
  }

  public DefaultAdHocRule(@Nullable SensorStorage storage) {
    super(storage);
  }

  @Override
  public DefaultAdHocRule severity(Severity severity) {
    this.severity = severity;
    return this;
  }

  @Override
  public String engineId() {
    return engineId;
  }

  @Override
  public String ruleId() {
    return ruleId;
  }

  @Override
  public String name() {
    return name;
  }

  @CheckForNull
  @Override
  public String description() {
    return description;
  }

  @Override
  public Severity severity() {
    return this.severity;
  }

  @Override
  public void doSave() {
    checkState(isNotBlank(engineId), "Engine id is mandatory on ad hoc rule");
    checkState(isNotBlank(ruleId), "Rule id is mandatory on ad hoc rule");
    checkState(isNotBlank(name), "Name is mandatory on every ad hoc rule");
    checkState(severity != null, "Severity is mandatory on every ad hoc rule");
    checkState(type != null, "Type is mandatory on every ad hoc rule");
    storage.store(this);
  }

  @Override
  public RuleType type() {
    return type;
  }

  @Override
  public DefaultAdHocRule engineId(String engineId) {
    this.engineId = engineId;
    return this;
  }

  @Override
  public DefaultAdHocRule ruleId(String ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  @Override
  public DefaultAdHocRule name(String name) {
    this.name = name;
    return this;
  }

  @Override
  public DefaultAdHocRule description(@Nullable String description) {
    this.description = description;
    return this;
  }

  @Override
  public DefaultAdHocRule type(RuleType type) {
    this.type = type;
    return this;
  }

}
