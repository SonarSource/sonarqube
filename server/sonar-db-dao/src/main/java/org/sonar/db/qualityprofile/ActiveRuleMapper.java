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
package org.sonar.db.qualityprofile;

import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.rule.RuleStatus;
import org.sonar.db.KeyLongValue;

public interface ActiveRuleMapper {

  void insert(ActiveRuleDto dto);

  void update(ActiveRuleDto dto);

  void delete(int activeRuleId);

  void deleteByRuleProfileUuids(@Param("rulesProfileUuids") Collection<String> rulesProfileUuids);

  void deleteByIds(@Param("ids") Collection<Integer> ids);

  @CheckForNull
  ActiveRuleDto selectByKey(@Param("ruleProfileUuid") String ruleProfileUuid, @Param("repository") String repository, @Param("rule") String rule);

  List<ActiveRuleDto> selectByKeys(@Param("keys") List<ActiveRuleKey> keys);

  List<OrgActiveRuleDto> selectByRuleId(@Param("organizationUuid") String organizationUuid, @Param("ruleId") int ruleId);

  List<ActiveRuleDto> selectByRuleIdOfAllOrganizations(int ruleId);

  List<OrgActiveRuleDto> selectByRuleIds(@Param("organizationUuid") String organizationUuid, @Param("ruleIds") List<Integer> partitionOfRuleIds);

  List<OrgActiveRuleDto> selectByProfileUuid(String uuid);

  List<OrgActiveRuleDto> selectByTypeAndProfileUuids(@Param("types") List<Integer> types, @Param("profileUuids") List<String> uuids);

  List<ActiveRuleDto> selectByRuleProfileUuid(@Param("ruleProfileUuid") String uuid);

  List<ActiveRuleDto> selectByRuleIdsAndRuleProfileUuids(
    @Param("ruleIds") Collection<Integer> ruleIds,
    @Param("ruleProfileUuids") Collection<String> ruleProfileUuids);

  void insertParameter(ActiveRuleParamDto dto);

  void updateParameter(ActiveRuleParamDto dto);

  void deleteParameters(int activeRuleId);

  void deleteParametersByRuleProfileUuids(@Param("rulesProfileUuids") Collection<String> rulesProfileUuids);

  void deleteParameter(int activeRuleParamId);

  void deleteParamsByActiveRuleIds(@Param("activeRuleIds") Collection<Integer> activeRuleIds);

  List<ActiveRuleParamDto> selectParamsByActiveRuleId(int activeRuleId);

  List<ActiveRuleParamDto> selectParamsByActiveRuleIds(@Param("ids") List<Integer> ids);

  List<KeyLongValue> countActiveRulesByQuery(@Param("organizationUuid") String organizationUuid, @Param("profileUuids") List<String> profileUuids,
    @Nullable @Param("ruleStatus") RuleStatus ruleStatus, @Param("inheritance") String inheritance);

  void scrollAllForIndexing(ResultHandler<IndexedActiveRuleDto> handler);

  void scrollByIdsForIndexing(@Param("ids") Collection<Long> ids, ResultHandler<IndexedActiveRuleDto> handler);

  void scrollByRuleProfileUuidForIndexing(@Param("ruleProfileUuid") String ruleProfileUuid, ResultHandler<IndexedActiveRuleDto> handler);
}
