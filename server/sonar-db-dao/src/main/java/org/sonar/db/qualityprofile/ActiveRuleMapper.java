/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

  void delete(String activeRuleUuid);

  void deleteByRuleProfileUuids(@Param("rulesProfileUuids") Collection<String> rulesProfileUuids);

  void deleteByUuids(@Param("uuids") Collection<String> uuids);

  @CheckForNull
  ActiveRuleDto selectByKey(@Param("ruleProfileUuid") String ruleProfileUuid, @Param("repository") String repository, @Param("rule") String rule);

  List<OrgActiveRuleDto> selectOrgByRuleUuid(@Param("organizationUuid") String organizationUuid, @Param("ruleUuid") String ruleUuid);

  List<ActiveRuleDto> selectByRuleUuid(String ruleUuid);

  List<ActiveRuleDto> selectByRepository(@Param("repositoryKey") String repositoryKey,
    @Param("repositoryLanguage") String repositoryLanguage);

  List<OrgActiveRuleDto> selectByRuleUuids(@Param("ruleUuids") List<String> partitionOfRuleUuids);

  List<OrgActiveRuleDto> selectByProfileUuid(String uuid);

  List<OrgActiveRuleDto> selectByTypeAndProfileUuids(@Param("types") List<Integer> types, @Param("profileUuids") List<String> uuids);

  List<ActiveRuleDto> selectByRuleProfileUuid(@Param("ruleProfileUuid") String uuid);

  List<ActiveRuleDto> selectByRuleUuidsAndRuleProfileUuids(
    @Param("ruleUuids") Collection<String> ruleUuids,
    @Param("ruleProfileUuids") Collection<String> ruleProfileUuids);

  List<OrgActiveRuleDto> selectPrioritizedRules(@Param("ruleProfileUuids") Collection<String> ruleProfileUuids);

  void insertParameter(ActiveRuleParamDto dto);

  void updateParameter(ActiveRuleParamDto dto);

  void deleteParameters(String activeRuleUuid);

  void deleteParametersByRuleProfileUuids(@Param("rulesProfileUuids") Collection<String> rulesProfileUuids);

  void deleteParameter(String activeRuleParamUuid);

  void deleteParamsByActiveRuleUuids(@Param("activeRuleUuids") Collection<String> activeRuleUuids);

  List<ActiveRuleParamDto> selectParamsByActiveRuleUuid(String activeRuleUuid);

  List<ActiveRuleParamDto> selectParamsByActiveRuleUuids(@Param("uuids") List<String> uuids);

  List<KeyLongValue> countActiveRulesByQuery(@Param("profileUuids") List<String> profileUuids,
    @Nullable @Param("ruleStatus") RuleStatus ruleStatus, @Param("inheritance") String inheritance);

  void scrollAllForIndexing(ResultHandler<IndexedActiveRuleDto> handler);

  void scrollByUuidsForIndexing(@Param("uuids") Collection<String> uuids, ResultHandler<IndexedActiveRuleDto> handler);

  void scrollByRuleProfileUuidForIndexing(@Param("ruleProfileUuid") String ruleProfileUuid, ResultHandler<IndexedActiveRuleDto> handler);

  int countMissingRules(@Param("rulesProfileUuid") String rulesProfileUuid, @Param("compareToRulesProfileUuid") String compareToRulesProfileUuid);
}
