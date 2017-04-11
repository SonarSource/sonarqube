/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;
import org.sonar.api.rule.RuleStatus;
import org.sonar.db.KeyLongValue;

public interface ActiveRuleMapper {

  void insert(ActiveRuleDto dto);

  void update(ActiveRuleDto dto);

  void delete(int activeRuleId);

  void deleteByProfileKeys(@Param("profileKeys") Collection<String> profileKeys);

  ActiveRuleDto selectByKey(@Param("profileKey") String profileKey, @Param("repository") String repository, @Param("rule") String rule);

  List<ActiveRuleDto> selectByKeys(@Param("keys") List<ActiveRuleKey> keys);

  List<ActiveRuleDto> selectByRuleId(@Param("organizationUuid") String organizationUuid, @Param("ruleId") int ruleId);

  List<ActiveRuleDto> selectByRuleIdOfAllOrganizations(int ruleId);

  List<ActiveRuleDto> selectByRuleIds(@Param("organizationUuid") String organizationUuid, @Param("ruleIds") List<Integer> partitionOfRuleIds);

  List<ActiveRuleDto> selectByProfileKey(String key);

  void insertParameter(ActiveRuleParamDto dto);

  void updateParameter(ActiveRuleParamDto dto);

  void deleteParameters(int activeRuleId);

  void deleteParametersByProfileKeys(@Param("profileKeys") Collection<String> profileKeys);

  void deleteParameter(int activeRuleParamId);

  @CheckForNull
  ActiveRuleParamDto selectParamByActiveRuleAndKey(@Param("activeRuleId") int activeRuleId, @Param("key") String key);

  List<ActiveRuleParamDto> selectParamsByActiveRuleId(int activeRuleId);

  List<ActiveRuleParamDto> selectParamsByActiveRuleIds(@Param("ids") List<Integer> ids);

  List<ActiveRuleParamDto> selectAllParams();

  List<KeyLongValue> countActiveRulesByProfileKey(@Param("organizationUuid") String organizationUuid);

  List<KeyLongValue> countActiveRulesForRuleStatusByProfileKey(@Param("organizationUuid") String organizationUuid, @Param("ruleStatus") RuleStatus ruleStatus);

  List<KeyLongValue> countActiveRulesForInheritanceByProfileKey(@Param("organizationUuid") String organizationUuid, @Param("inheritance") String inheritance);
}
