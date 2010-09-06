/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch;

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.checks.profiles.Check;
import org.sonar.api.checks.profiles.CheckProfile;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckProfileProvider extends ProviderAdapter {

  private CheckProfile profile = null;

  public CheckProfile provide(RulesProfile ruleProfile) {
    if (profile == null) {
      profile = new CheckProfile(ruleProfile.getName(), ruleProfile.getLanguage());
      for (ActiveRule activeRule : ruleProfile.getActiveRules()) {
        Check check = toCheck(activeRule);
        profile.addCheck(check);
      }
    }
    return profile;
  }

  private Check toCheck(ActiveRule activeRule) {
    Check check = new Check(activeRule.getPluginName(), activeRule.getRuleKey());
    check.setPriority(activeRule.getPriority().toCheckPriority());
    check.setProperties(toParameters(activeRule.getActiveRuleParams()));
    return check;
  }

  private Map<String, String> toParameters(List<ActiveRuleParam> params) {
    Map<String, String> map = new HashMap<String, String>();
    if (params != null) {
      for (ActiveRuleParam param : params) {
        map.put(param.getRuleParam().getKey(), param.getValue());
      }
    }
    return map;
  }
}
