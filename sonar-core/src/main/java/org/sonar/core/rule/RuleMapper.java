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

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Select;
import org.sonar.api.rule.RuleKey;

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

  final String SELECT_FIELDS="r.id,\n" +
    "    r.plugin_rule_key as \"ruleKey\",\n" +
    "    r.plugin_name as \"repositoryKey\",\n" +
    "    r.description,\n" +
    "    r.description_format as \"descriptionFormat\",\n" +
    "    r.status,\n" +
    "    r.name,\n" +
    "    r.plugin_config_key as \"configKey\",\n" +
    "    r.priority as \"severity\",\n" +
    "    r.is_template as \"isTemplate\",\n" +
    "    r.language as \"language\",\n" +
    "    r.template_id as \"templateId\",\n" +
    "    r.note_data as \"noteData\",\n" +
    "    r.note_user_login as \"noteUserLogin\",\n" +
    "    r.note_created_at as \"noteCreatedAt\",\n" +
    "    r.note_updated_at as \"noteUpdatedAt\",\n" +
    "    r.characteristic_id as \"subCharacteristicId\",\n" +
    "    r.default_characteristic_id as \"defaultSubCharacteristicId\",\n" +
    "    r.remediation_function as \"remediationFunction\",\n" +
    "    r.default_remediation_function as \"defaultRemediationFunction\",\n" +
    "    r.remediation_coeff as \"remediationCoefficient\",\n" +
    "    r.default_remediation_coeff as \"defaultRemediationCoefficient\",\n" +
    "    r.remediation_offset as \"remediationOffset\",\n" +
    "    r.default_remediation_offset as \"defaultRemediationOffset\",\n" +
    "    r.effort_to_fix_description as \"effortToFixDescription\",\n" +
    "    r.tags as \"tagsField\",\n" +
    "    r.system_tags as \"systemTagsField\",\n" +
    "    r.created_at as \"createdAt\",\n" +
    "    r.updated_at as \"updatedAt\"";

  @Select("SELECT " + SELECT_FIELDS + " FROM rules r WHERE r.updated_at IS NULL or r.updated_at >= #{date} ")
  @Options(fetchSize = 200, useCache = false, flushCache = true)
  @Result(javaType = RuleDto.class)
  List<RuleDto> selectAfterDate(@Param("date") Timestamp timestamp);
}
