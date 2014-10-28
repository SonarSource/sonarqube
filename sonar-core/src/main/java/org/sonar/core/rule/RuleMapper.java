/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.rule;

import org.apache.ibatis.annotations.Param;
import org.sonar.api.rule.RuleKey;

import javax.annotation.Nullable;

import java.sql.Timestamp;
import java.util.List;

public interface RuleMapper {

  List<RuleDto> selectAll();

  List<RuleDto> selectEnablesAndNonManual();

  List<RuleDto> selectNonManual();

  List<RuleDto> selectBySubCharacteristicId(int characteristicId);

  RuleDto selectById(Integer id);

  RuleDto selectByKey(RuleKey ruleKey);

  RuleDto selectByName(String name);

  void update(RuleDto rule);

  void batchInsert(RuleDto rule);

  void insert(RuleDto rule);

  List<RuleParamDto> selectAllParams();

  List<RuleParamDto> selectParamsByRuleIds(@Param("ruleIds") List<Integer> ruleIds);

  List<RuleParamDto> selectParamsByRuleKey(RuleKey ruleKey);

  RuleParamDto selectParamByRuleAndKey(@Param("ruleId") Integer ruleId, @Param("key") String key);

  void insertParameter(RuleParamDto param);

  void updateParameter(RuleParamDto param);

  void deleteParameter(Integer paramId);

  List<RuleDto> selectAfterDate(@Nullable @Param("date") Timestamp timestamp);
}
