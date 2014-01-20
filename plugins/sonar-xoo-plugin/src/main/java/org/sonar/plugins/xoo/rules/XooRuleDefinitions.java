/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.xoo.rules;

import org.sonar.api.server.rule.RuleDefinitions;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.plugins.xoo.base.XooConstants;
import org.sonar.plugins.xoo.base.XooRuleKeys;

public class XooRuleDefinitions implements RuleDefinitions {

  private static final String TAG1 = "tag1", TAG2 = "tag2", TAG3 = "tag3", TAG4 = "tag4";

  @Override
  public void define(Context context) {
    final NewRepository xooRepository = context.newRepository(XooConstants.REPOSITORY_KEY, XooConstants.LANGUAGE_KEY).setName("Xoo");


    xooRepository.newRule(XooRuleKeys.RULE_MINIMAL)
      .setName("Minimal rule")
      .setHtmlDescription("Minimal rule, with only required fields");

    final NewRule ruleWithParams = xooRepository.newRule(XooRuleKeys.RULE_WITH_PARAMS)
      .setName("Rule with parameters")
      .setHtmlDescription("This rule defines parameters, one for each supported type");
    ruleWithParams.newParam("string")
      .setName("String")
      .setType(RuleParamType.STRING)
      .setDescription("A string parameter");
    ruleWithParams.newParam("text")
      .setName("Text")
      .setType(RuleParamType.TEXT)
      .setDescription("A text parameter");
    ruleWithParams.newParam("bool")
      .setName("Boolean")
      .setType(RuleParamType.BOOLEAN)
      .setDescription("A boolean parameter");
    ruleWithParams.newParam("float")
      .setName("Float")
      .setType(RuleParamType.FLOAT)
      .setDescription("A float parameter");
    ruleWithParams.newParam("int")
      .setName("Integer")
      .setType(RuleParamType.INTEGER)
      .setDescription("An integer parameter");

    xooRepository.newRule(XooRuleKeys.RULE_WITH_TAGS1)
      .setName("Tag 1")
      .setHtmlDescription("Rule with tag <code>tag1</code>")
      .setTags(TAG1);
    xooRepository.newRule(XooRuleKeys.RULE_WITH_TAGS12)
      .setName("Tags 1 and 2")
      .setHtmlDescription("Rule with tags <code>tag1</code> and <code>tag2</code>")
      .setTags(TAG1, TAG2);
    xooRepository.newRule(XooRuleKeys.RULE_WITH_TAGS123)
      .setName("Tags 1, 2 and 3")
      .setHtmlDescription("Rule with tags <code>tag1</code>, <code>tag2</code> and <code>tag3</code>")
      .setTags(TAG1, TAG2, TAG3);
    xooRepository.newRule(XooRuleKeys.RULE_WITH_TAGS23)
      .setName("Tags 2 and 3")
      .setHtmlDescription("Rule with tags <code>tag2</code> and <code>tag3</code>")
      .setTags(TAG2, TAG3);
    xooRepository.newRule(XooRuleKeys.RULE_WITH_TAGS34)
      .setName("Tags 3 and 4")
      .setHtmlDescription("Rule with tags <code>tag3</code> and <code>tag4</code>")
      .setTags(TAG3, TAG4);

    xooRepository.done();
  }

}
