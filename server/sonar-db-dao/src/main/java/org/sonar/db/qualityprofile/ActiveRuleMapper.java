/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;

public interface ActiveRuleMapper {

  void insert(ActiveRuleDto dto);

  void update(ActiveRuleDto dto);

  void delete(int activeRuleId);

  ActiveRuleDto selectByKey(@Param("profileKey") String profileKey, @Param("repository") String repository, @Param("rule") String rule);

  List<ActiveRuleDto> selectByKeys(@Param("keys") List<ActiveRuleKey> keys);

  List<ActiveRuleDto> selectByRuleId(int ruleId);

  List<ActiveRuleDto> selectByRuleIds(@Param("ruleIds") List<Integer> partitionOfRuleIds);

  List<ActiveRuleDto> selectByProfileKey(String key);

  List<ActiveRuleDto> selectAll();

  void insertParameter(ActiveRuleParamDto dto);

  void updateParameter(ActiveRuleParamDto dto);

  void deleteParameters(int activeRuleId);

  void deleteParameter(int activeRuleParamId);

  @CheckForNull
  ActiveRuleParamDto selectParamByActiveRuleAndKey(@Param("activeRuleId") int activeRuleId, @Param("key") String key);

  List<ActiveRuleParamDto> selectParamsByActiveRuleId(int activeRuleId);

  List<ActiveRuleParamDto> selectParamsByActiveRuleIds(@Param("ids") List<Integer> ids);

  List<ActiveRuleParamDto> selectAllParams();
}
