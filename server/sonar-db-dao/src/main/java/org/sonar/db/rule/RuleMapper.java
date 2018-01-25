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
package org.sonar.db.rule;

import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.db.es.RuleExtensionId;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleQuery;

public interface RuleMapper {

  List<RuleDto> selectAll(@Param("organizationUuid") String organizationUuid);

  List<RuleDefinitionDto> selectAllDefinitions();

  void selectEnabled(ResultHandler<RuleDefinitionDto> resultHandler);

  RuleDto selectById(@Param("organizationUuid") String organizationUuid, @Param("id") long id);

  RuleDefinitionDto selectDefinitionById(long id);

  List<RuleDto> selectByIds(@Param("organizationUuid") String organizationUuid, @Param("ids") List<Integer> ids);

  List<RuleDefinitionDto> selectDefinitionByIds(@Param("ids") List<Integer> ids);

  RuleDto selectByKey(@Param("organizationUuid") String organizationUuid, @Param("ruleKey") RuleKey ruleKey);

  RuleDefinitionDto selectDefinitionByKey(RuleKey ruleKey);

  RuleMetadataDto selectMetadataByKey(@Param("ruleKey") RuleKey ruleKey, @Param("organizationUuid") String organizationUuid);

  List<RuleDto> selectByKeys(@Param("organizationUuid") String organizationUuid, @Param("ruleKeys") List<RuleKey> keys);

  List<RuleDefinitionDto> selectDefinitionByKeys(@Param("ruleKeys") List<RuleKey> keys);

  void scrollIndexingRules(ResultHandler<RuleForIndexingDto> handler);

  List<RuleForIndexingDto> selectIndexingRulesByIds(@Param("ruleIds") List<Integer> ruleIds);

  void scrollIndexingRuleExtensions(ResultHandler<RuleExtensionForIndexingDto> handler);

  List<RuleExtensionForIndexingDto> selectIndexingRuleExtensionsByIds(@Param("ruleExtensionIds") List<RuleExtensionId> ruleExtensionIds);

  List<RuleDto> selectByQuery(@Param("organizationUuid") String organizationUuid, @Param("query") RuleQuery ruleQuery);

  void insertDefinition(RuleDefinitionDto ruleDefinitionDto);

  void updateDefinition(RuleDefinitionDto ruleDefinitionDto);

  int countMetadata(RuleMetadataDto ruleMetadataDto);

  void insertMetadata(RuleMetadataDto ruleMetadataDto);

  void updateMetadata(RuleMetadataDto ruleMetadataDto);

  List<RuleParamDto> selectParamsByRuleIds(@Param("ruleIds") List<Integer> ruleIds);

  List<RuleParamDto> selectParamsByRuleKey(RuleKey ruleKey);

  List<RuleParamDto> selectParamsByRuleKeys(@Param("ruleKeys") List<RuleKey> ruleKeys);

  void insertParameter(RuleParamDto param);

  void updateParameter(RuleParamDto param);

  void deleteParameter(Integer paramId);

  Set<DeprecatedRuleKeyDto> selectAllDeprecatedRuleKeys();

  void deleteDeprecatedRuleKeys(@Param("uuids") List<String> uuids);

  void insertDeprecatedRuleKey(DeprecatedRuleKeyDto deprecatedRuleKeyDto);
}
