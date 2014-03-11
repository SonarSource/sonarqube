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

import org.sonar.wsclient.services.Profile;
import org.sonar.wsclient.services.WSUtils;

public class ProfileUnmarshaller extends AbstractUnmarshaller<Profile> {

  @Override
  protected Profile parse(Object json) {
    WSUtils utils = WSUtils.getINSTANCE();
    Profile profile = new Profile();
    Boolean defaultProfile = utils.getBoolean(json, "default");
    profile
        .setLanguage(utils.getString(json, "language"))
        .setName(utils.getString(json, "name"))
        .setDefaultProfile(defaultProfile != null ? defaultProfile : false)
        .setParentName(utils.getString(json, "parent"));

    parseRules(utils, profile, json);
    return profile;
  }

  private void parseRules(WSUtils utils, Profile profile, Object json) {
    Object rulesJson = utils.getField(json, "rules");
    if (rulesJson != null) {
      for (int i = 0; i < utils.getArraySize(rulesJson); i++) {
        Object ruleJson = utils.getArrayElement(rulesJson, i);
        if (ruleJson != null) {
          Profile.Rule rule = new Profile.Rule();
          rule.setKey(utils.getString(ruleJson, "key"));
          rule.setRepository(utils.getString(ruleJson, "repo"));
          rule.setSeverity(utils.getString(ruleJson, "severity"));
          rule.setInheritance(utils.getString(ruleJson, "inheritance"));

          parseRuleParameters(utils, rule, ruleJson);
          profile.addRule(rule);
        }
      }
    }
  }

  private void parseRuleParameters(WSUtils utils, Profile.Rule rule, Object ruleJson) {
    Object paramsJson = utils.getField(ruleJson, "params");
    if (paramsJson != null) {
      for (int indexParam = 0; indexParam < utils.getArraySize(paramsJson); indexParam++) {
        Object paramJson = utils.getArrayElement(paramsJson, indexParam);
        rule.addParameter(utils.getString(paramJson, "key"), utils.getString(paramJson, "value"));
      }
    }
  }

}
