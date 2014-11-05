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
package org.sonar.core.persistence.migration.v45;

import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;

public interface Migration45Mapper {

  @Select("SELECT rules_parameters.id, rules_parameters.rule_id as \"ruleId\", rules_parameters.name as \"name\", rules_parameters.param_type as \"type\", " +
    "  rules_parameters.default_value as \"defaultValue\", rules_parameters.description, rules.template_id as \"ruleTemplateId\" " +
    "FROM rules_parameters " +
    "  INNER JOIN rules ON rules.id = rules_parameters.rule_id " +
    "WHERE rules.is_template = ${_true}")
  @Result(javaType = RuleParameter.class)
  List<RuleParameter> selectAllTemplateRuleParameters();

  @Select("SELECT rules_parameters.id, rules_parameters.rule_id as \"ruleId\", rules_parameters.name as \"name\", rules_parameters.param_type as \"type\", " +
    "  rules_parameters.default_value as \"defaultValue\", rules_parameters.description, rules.template_id as \"ruleTemplateId\" " +
    "FROM rules_parameters " +
    "  INNER JOIN rules ON rules.id = rules_parameters.rule_id " +
    "WHERE rules.template_id IS NOT NULL")
  @Result(javaType = RuleParameter.class)
  List<RuleParameter> selectAllCustomRuleParameters();

  @Select("SELECT id, plugin_rule_key as \"ruleKey\", plugin_name as \"repositoryKey\", is_template as \"isTemplate\", template_id as \"templateId\"" +
    "FROM rules " +
    "WHERE rules.template_id IS NOT NULL")
  @Result(javaType = Rule.class)
  List<Rule> selectAllCustomRules();

  @Insert("INSERT INTO rules_parameters (rule_id, name, param_type, default_value, description)" +
    " VALUES (#{ruleId}, #{name}, #{type}, #{defaultValue}, #{description})")
  @Options(useGeneratedKeys = false)
  void insertRuleParameter(RuleParameter ruleParameter);

  @Insert("UPDATE rules SET updated_at=#{date} WHERE id=#{id}")
  @Options(useGeneratedKeys = false)
  void updateRuleUpdateAt(@Param("id") Integer ruleId, @Param("date") Date updatedAt);

}
