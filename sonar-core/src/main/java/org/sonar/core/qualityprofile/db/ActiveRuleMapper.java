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

package org.sonar.core.qualityprofile.db;

import org.apache.ibatis.annotations.Param;

import javax.annotation.CheckForNull;

import java.util.List;

public interface ActiveRuleMapper {

  void insert(ActiveRuleDto dto);

  void update(ActiveRuleDto dto);

  void delete(int activeRuleId);

  void deleteFromRule(int ruleId);

  void deleteFromProfile(int profileId);

  @CheckForNull
  ActiveRuleDto selectById(Integer id);

  @CheckForNull
  ActiveRuleDto selectByProfileAndRule(@Param("profileId") int profileId, @Param("ruleId") int ruleId);

  List<ActiveRuleDto> selectByRuleId(int ruleId);

  List<ActiveRuleDto> selectByProfileId(int profileId);

  List<ActiveRuleDto> selectAll();

  void insertParameter(ActiveRuleParamDto dto);

  void updateParameter(ActiveRuleParamDto dto);

  void deleteParameters(int activeRuleId);

  void deleteParameter(int activeRuleParamId);

  void deleteParametersWithParamId(int id);

  void deleteParametersFromProfile(int profileId);

  @CheckForNull
  ActiveRuleParamDto selectParamById(int activeRuleParamId);

  @CheckForNull
  ActiveRuleParamDto selectParamByActiveRuleAndKey(@Param("activeRuleId") int activeRuleId, @Param("key") String key);

  List<ActiveRuleParamDto> selectParamsByActiveRuleId(int activeRuleId);

  List<ActiveRuleParamDto> selectParamsByProfileId(int profileId);

  List<ActiveRuleParamDto> selectAllParams();


}
