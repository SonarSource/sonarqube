/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.core.qualityprofile.db;

import org.apache.ibatis.annotations.Param;

import javax.annotation.CheckForNull;

import java.util.List;

public interface ActiveRuleMapper {

  void insert(ActiveRuleDto dto);

  void update(ActiveRuleDto dto);

  void delete(Integer activeRuleId);

  void deleteFromRule(Integer ruleId);

  @CheckForNull
  ActiveRuleDto selectById(Integer id);

  @CheckForNull
  ActiveRuleDto selectByProfileAndRule(@Param("profileId") Integer profileId, @Param("ruleId") Integer ruleId);

  List<ActiveRuleDto> selectByRuleId(Integer ruleId);

  List<ActiveRuleDto> selectByProfileId(int profileId);

  List<ActiveRuleDto> selectAll();

  void insertParameter(ActiveRuleParamDto dto);

  void updateParameter(ActiveRuleParamDto dto);

  void deleteParameters(Integer activeRuleId);

  void deleteParameter(Integer activeRuleParamId);

  void deleteParametersWithParamId(Integer id);

  @CheckForNull
  ActiveRuleParamDto selectParamById(Integer activeRuleParamId);

  @CheckForNull
  ActiveRuleParamDto selectParamByActiveRuleAndKey(@Param("activeRuleId") Integer activeRuleId, @Param("key") String key);

  List<ActiveRuleParamDto> selectParamsByActiveRuleId(Integer activeRuleId);

  List<ActiveRuleParamDto> selectAllParams();


}
