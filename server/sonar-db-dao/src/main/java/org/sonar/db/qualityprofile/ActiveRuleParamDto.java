/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.qualityprofile;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.sonar.db.rule.RuleParamDto;

public class ActiveRuleParamDto {

  private String uuid;
  private String activeRuleUuid;
  private String rulesParameterUuid;
  private String kee;
  private String value;

  public String getUuid() {
    return uuid;
  }

  public ActiveRuleParamDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getActiveRuleUuid() {
    return activeRuleUuid;
  }

  public ActiveRuleParamDto setActiveRuleUuid(String activeRuleUuid) {
    this.activeRuleUuid = activeRuleUuid;
    return this;
  }

  public String getRulesParameterUuid() {
    return rulesParameterUuid;
  }

  // TODO set private or drop
  public ActiveRuleParamDto setRulesParameterUuid(String rulesParameterUuid) {
    this.rulesParameterUuid = rulesParameterUuid;
    return this;
  }

  public String getKey() {
    return kee;
  }

  public ActiveRuleParamDto setKey(String key) {
    this.kee = key;
    return this;
  }

  public String getValue() {
    return value;
  }

  public ActiveRuleParamDto setValue(String value) {
    this.value = value;
    return this;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }

  public static ActiveRuleParamDto createFor(RuleParamDto param) {
    Preconditions.checkArgument(param.getUuid() != null, "Parameter is not persisted");
    return new ActiveRuleParamDto()
      .setKey(param.getName())
      .setRulesParameterUuid(param.getUuid());
  }

  public static Map<String, ActiveRuleParamDto> groupByKey(Collection<ActiveRuleParamDto> params) {
    Map<String, ActiveRuleParamDto> result = new HashMap<>();
    for (ActiveRuleParamDto param : params) {
      result.put(param.getKey(), param);
    }
    return result;
  }
}
