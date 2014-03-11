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
package org.sonar.wsclient.unmarshallers;

import org.sonar.wsclient.services.Rule;
import org.sonar.wsclient.services.RuleParam;
import org.sonar.wsclient.services.WSUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 2.5
 */
public class RuleUnmarshaller extends AbstractUnmarshaller<Rule> {

  private static final String DESCRIPTION = "description";

  @Override
  protected Rule parse(Object json) {
    Rule rule = new Rule();
    parseRuleFields(json, rule);
    parseParams(json, rule);
    return rule;
  }

  private void parseRuleFields(Object json, Rule rule) {
    WSUtils utils = WSUtils.getINSTANCE();

    rule.setTitle(utils.getString(json, "title"))
        .setKey(utils.getString(json, "key"))
        .setConfigKey(utils.getString(json, "config_key"))
        .setRepository(utils.getString(json, "plugin"))
        .setDescription(utils.getString(json, DESCRIPTION))
        .setSeverity(utils.getString(json, "priority"))
        .setActive("ACTIVE".equals(utils.getString(json, "status")));
  }

  private void parseParams(Object json, Rule rule) {
    WSUtils utils = WSUtils.getINSTANCE();

    Object paramsJson = utils.getField(json, "params");
    if (paramsJson != null) {
      rule.setParams(parseParams(paramsJson));
    }
  }

  private List<RuleParam> parseParams(Object paramsJson) {
    WSUtils utils = WSUtils.getINSTANCE();

    List<RuleParam> ruleParams = new ArrayList<RuleParam>();
    int len = utils.getArraySize(paramsJson);
    for (int i = 0; i < len; i++) {
      Object paramJson = utils.getArrayElement(paramsJson, i);
      if (paramJson != null) {
        RuleParam param = parseParam(paramJson);
        ruleParams.add(param);
      }
    }
    return ruleParams;
  }

  private RuleParam parseParam(Object json) {
    WSUtils utils = WSUtils.getINSTANCE();

    RuleParam param = new RuleParam();
    param.setName(utils.getString(json, "name"))
        .setDescription(utils.getString(json, DESCRIPTION))
        .setValue(utils.getString(json, "value"));
    return param;
  }

}
