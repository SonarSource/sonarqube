/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleQuery;

public interface RuleMapper {

  List<RuleDto> selectAll();

  List<RuleDefinitionDto> selectAllDefinitions();

  void selectEnabled(ResultHandler<RuleDefinitionDto> resultHandler);

  RuleDto selectByUuid(@Param("uuid") String uuid);

  RuleDefinitionDto selectDefinitionByUuid(String uuid);

  List<RuleDto> selectByUuids(@Param("uuids") List<String> uuids);

  List<RuleDefinitionDto> selectDefinitionByUuids(@Param("uuids") List<String> uuids);

  RuleDto selectByKey(@Param("ruleKey") RuleKey ruleKey);

  RuleDefinitionDto selectDefinitionByKey(RuleKey ruleKey);

  RuleMetadataDto selectMetadataByKey(@Param("ruleKey") RuleKey ruleKey);

  List<RuleMetadataDto> selectMetadataByKeys(@Param("ruleKeys") List<RuleKey> keys);

  List<RuleDto> selectByKeys(@Param("ruleKeys") List<RuleKey> keys);

  List<RuleDefinitionDto> selectDefinitionByKeys(@Param("ruleKeys") List<RuleKey> keys);

  void scrollIndexingRules(ResultHandler<RuleForIndexingDto> handler);

  List<RuleForIndexingDto> selectIndexingRulesByUuids(@Param("ruleUuids") List<String> ruleUuids);

  List<RuleExtensionForIndexingDto> selectIndexingRuleExtensionsByIds(@Param("ruleExtensionIds") List<String> ruleExtensionIds);

  List<RuleDto> selectByQuery(@Param("query") RuleQuery ruleQuery);

  List<RuleDto> selectByTypeAndLanguages(@Param("types") List<Integer> types, @Param("languages") List<String> languages);

  void insertDefinition(RuleDefinitionDto ruleDefinitionDto);

  void updateDefinition(RuleDefinitionDto ruleDefinitionDto);

  int countMetadata(RuleMetadataDto ruleMetadataDto);

  void insertMetadata(RuleMetadataDto ruleMetadataDto);

  void updateMetadata(RuleMetadataDto ruleMetadataDto);

  List<RuleParamDto> selectParamsByRuleUuids(@Param("ruleUuids") List<String> ruleUuids);

  List<RuleParamDto> selectParamsByRuleKey(RuleKey ruleKey);

  List<RuleParamDto> selectParamsByRuleKeys(@Param("ruleKeys") List<RuleKey> ruleKeys);

  List<RuleParamDto> selectAllRuleParams();

  void insertParameter(RuleParamDto param);

  void updateParameter(RuleParamDto param);

  void deleteParameter(String paramUuid);

  Set<DeprecatedRuleKeyDto> selectAllDeprecatedRuleKeys();

  Set<DeprecatedRuleKeyDto> selectDeprecatedRuleKeysByRuleUuids(@Param("ruleUuids") Collection<String> ruleUuids);

  void deleteDeprecatedRuleKeys(@Param("uuids") List<String> uuids);

  void insertDeprecatedRuleKey(DeprecatedRuleKeyDto deprecatedRuleKeyDto);
}
