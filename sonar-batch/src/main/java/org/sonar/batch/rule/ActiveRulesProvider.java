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
package org.sonar.batch.rule;

import org.sonar.api.utils.log.Profiler;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarqube.ws.Rules.Rule.Param;
import org.sonarqube.ws.Rules.Rule;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.rule.RuleKey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the rules that are activated on the Quality profiles
 * used by the current project and builds {@link org.sonar.api.batch.rule.ActiveRules}.
 */
public class ActiveRulesProvider extends ProviderAdapter {
  private static final Logger LOG = Loggers.get(ActiveRulesProvider.class);
  private static final String LOG_MSG = "Load active rules";
  private ActiveRules singleton = null;

  public ActiveRules provide(ActiveRulesLoader loader, ModuleQProfiles qProfiles) {
    if (singleton == null) {
      Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
      singleton = load(loader, qProfiles);
      profiler.stopInfo();
    }
    return singleton;
  }

  private static ActiveRules load(ActiveRulesLoader loader, ModuleQProfiles qProfiles) {

    Collection<String> qProfileKeys = getKeys(qProfiles);
    Map<String, Rule> loadedRulesByKey = new HashMap<>();

    try {
      for (String qProfileKey : qProfileKeys) {
        Collection<Rule> qProfileRules;
        qProfileRules = load(loader, qProfileKey);

        for (Rule r : qProfileRules) {
          if (!loadedRulesByKey.containsKey(r.getKey())) {
            loadedRulesByKey.put(r.getKey(), r);
          }
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Error loading active rules", e);
    }

    return transform(loadedRulesByKey.values());
  }

  private static ActiveRules transform(Collection<Rule> loadedRules) {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();

    for (Rule activeRule : loadedRules) {
      NewActiveRule newActiveRule = builder.create(RuleKey.of(activeRule.getRepo(), activeRule.getKey()));
      newActiveRule.setName(activeRule.getName());
      newActiveRule.setSeverity(activeRule.getSeverity());
      newActiveRule.setLanguage(activeRule.getLang());
      newActiveRule.setInternalKey(activeRule.getInternalKey());
      newActiveRule.setTemplateRuleKey(activeRule.getTemplateKey());

      // load parameters
      for (Param param : activeRule.getParams().getParamsList()) {
        newActiveRule.setParam(param.getKey(), param.getDefaultValue());
      }

      newActiveRule.activate();
    }
    return builder.build();
  }

  private static List<Rule> load(ActiveRulesLoader loader, String qProfileKey) throws IOException {
    return loader.load(qProfileKey, null);
  }

  private static Collection<String> getKeys(ModuleQProfiles qProfiles) {
    List<String> keys = new ArrayList<>(qProfiles.findAll().size());

    for (QProfile qp : qProfiles.findAll()) {
      keys.add(qp.getKey());
    }

    return keys;
  }
}
