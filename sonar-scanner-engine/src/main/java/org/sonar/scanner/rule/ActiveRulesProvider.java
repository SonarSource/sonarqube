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
package org.sonar.scanner.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;

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
    Map<RuleKey, LoadedActiveRule> loadedRulesByKey = new HashMap<>();

    for (String qProfileKey : qProfileKeys) {
      Collection<LoadedActiveRule> qProfileRules;
      qProfileRules = load(loader, qProfileKey);

      for (LoadedActiveRule r : qProfileRules) {
        if (!loadedRulesByKey.containsKey(r.getRuleKey())) {
          loadedRulesByKey.put(r.getRuleKey(), r);
        }
      }
    }

    return transform(loadedRulesByKey.values());
  }

  private static ActiveRules transform(Collection<LoadedActiveRule> loadedRules) {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();

    for (LoadedActiveRule activeRule : loadedRules) {
      NewActiveRule newActiveRule = builder.create(activeRule.getRuleKey());
      newActiveRule.setName(activeRule.getName());
      newActiveRule.setSeverity(activeRule.getSeverity());
      newActiveRule.setCreatedAt(activeRule.getCreatedAt());
      newActiveRule.setLanguage(activeRule.getLanguage());
      newActiveRule.setInternalKey(activeRule.getInternalKey());
      newActiveRule.setTemplateRuleKey(activeRule.getTemplateRuleKey());

      // load parameters
      if (activeRule.getParams() != null) {
        for (Map.Entry<String, String> params : activeRule.getParams().entrySet()) {
          newActiveRule.setParam(params.getKey(), params.getValue());
        }
      }

      newActiveRule.activate();
    }
    return builder.build();
  }

  private static List<LoadedActiveRule> load(ActiveRulesLoader loader, String qProfileKey) {
    return loader.load(qProfileKey);
  }

  private static Collection<String> getKeys(ModuleQProfiles qProfiles) {
    List<String> keys = new ArrayList<>(qProfiles.findAll().size());

    for (QProfile qp : qProfiles.findAll()) {
      keys.add(qp.getKey());
    }

    return keys;
  }
}
