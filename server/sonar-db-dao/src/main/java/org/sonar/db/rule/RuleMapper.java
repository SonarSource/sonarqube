/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleQuery;
import org.sonar.db.Pagination;
import org.sonar.db.issue.ImpactDto;

public interface RuleMapper {

  List<RuleDto> selectAll();

  List<RuleDto> selectEnabled();

  RuleDto selectByUuid(@Param("uuid") String uuid);

  List<RuleDto> selectByUuids(@Param("uuids") List<String> uuids);

  RuleDto selectByKey(@Param("ruleKey") RuleKey ruleKey);

  List<RuleDto> selectByKeys(@Param("ruleKeys") List<RuleKey> keys);

  List<RuleExtensionForIndexingDto> selectIndexingRuleExtensionsByIds(@Param("ruleExtensionIds") List<String> ruleExtensionIds);

  List<RuleDto> selectByQuery(@Param("query") RuleQuery ruleQuery);

  List<RuleDto> selectByTypeAndLanguages(@Param("types") List<Integer> types, @Param("languages") List<String> languages);

  List<RuleDto> selectByLanguage(@Param("language") String language);

  Long countByLanguage(@Param("language") String language);

  void insertRule(RuleDto ruleDefinitionDto);

  void insertRuleDescriptionSection(@Param("ruleUuid") String ruleUuid, @Param("dto") RuleDescriptionSectionDto ruleDescriptionSectionDto);

  void insertRuleDefaultImpact(@Param("ruleUuid") String ruleUuid, @Param("dto") ImpactDto ruleDefaultImpactDto);

  void insertRuleTag(@Param("ruleUuid") String ruleUuid, @Param("value") String value, @Param("isSystemTag") boolean isSystemTag);

  void updateRule(RuleDto ruleDefinitionDto);

  void deleteRuleDescriptionSection(String ruleUuid);

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

  void deleteRuleDefaultImpacts(String ruleUuid);

  void deleteRuleTags(String ruleUuid);

  List<String> selectTags(@Param("query") String query, @Param("pagination") Pagination pagination);
}
