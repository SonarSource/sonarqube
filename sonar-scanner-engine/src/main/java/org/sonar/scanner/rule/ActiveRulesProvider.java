/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  public ActiveRules provide(ActiveRulesLoader loader, QualityProfiles qProfiles) {
    if (singleton == null) {
      Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
      singleton = load(loader, qProfiles);
      profiler.stopInfo();
    }
    return singleton;
  }

  private static ActiveRules load(ActiveRulesLoader loader, QualityProfiles qProfiles) {

    Collection<String> qProfileKeys = getKeys(qProfiles);
    Set<RuleKey> loadedRulesKey = new HashSet<>();
    ActiveRulesBuilder builder = new ActiveRulesBuilder();

    for (String qProfileKey : qProfileKeys) {
      Collection<LoadedActiveRule> qProfileRules = load(loader, qProfileKey);

      for (LoadedActiveRule r : qProfileRules) {
        if (!loadedRulesKey.contains(r.getRuleKey())) {
          loadedRulesKey.add(r.getRuleKey());
          builder.addRule(transform(r, qProfileKey));
        }
      }
    }

    return builder.build();
  }

  private static NewActiveRule transform(LoadedActiveRule activeRule, String qProfileKey) {
    NewActiveRule.Builder builder = new NewActiveRule.Builder();
    builder
      .setRuleKey(activeRule.getRuleKey())
      .setName(activeRule.getName())
      .setSeverity(activeRule.getSeverity())
      .setCreatedAt(activeRule.getCreatedAt())
      .setUpdatedAt(activeRule.getUpdatedAt())
      .setLanguage(activeRule.getLanguage())
      .setInternalKey(activeRule.getInternalKey())
      .setTemplateRuleKey(activeRule.getTemplateRuleKey())
      .setQProfileKey(qProfileKey);
    // load parameters
    if (activeRule.getParams() != null) {
      for (Map.Entry<String, String> params : activeRule.getParams().entrySet()) {
        builder.setParam(params.getKey(), params.getValue());
      }
    }

    return builder.build();
  }

  private static List<LoadedActiveRule> load(ActiveRulesLoader loader, String qProfileKey) {
    return loader.load(qProfileKey);
  }

  private static Collection<String> getKeys(QualityProfiles qProfiles) {
    List<String> keys = new ArrayList<>(qProfiles.findAll().size());

    for (QProfile qp : qProfiles.findAll()) {
      keys.add(qp.getKey());
    }

    return keys;
  }
}
