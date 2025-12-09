/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.Map;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.scanner.bootstrap.ScannerProperties;
import org.springframework.context.annotation.Bean;

/**
 * Loads the rules that are activated on the Quality profiles
 * used by the current project and builds {@link org.sonar.api.batch.rule.ActiveRules}.
 */
public class ActiveRulesProvider {
  private static final Logger LOG = Loggers.get(ActiveRulesProvider.class);
  private static final String LOG_MSG = "Load active rules";

  @Bean("ActiveRules")
  public DefaultActiveRules provide(ActiveRulesLoader loader, ScannerProperties props) {
    Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
    DefaultActiveRules activeRules = load(loader, props.getProjectKey());
    profiler.stopInfo();
    return activeRules;
  }

  private static DefaultActiveRules load(ActiveRulesLoader loader, String projectKey) {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    loader.load(projectKey).stream()
      .map(ActiveRulesProvider::transform)
      .forEach(builder::addRule);
    return builder.build();
  }

  private static NewActiveRule transform(LoadedActiveRule activeRule) {
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
      .setQProfileKey(activeRule.getQProfileKey())
      .setDeprecatedKeys(activeRule.getDeprecatedKeys());
    // load parameters
    if (activeRule.getParams() != null) {
      for (Map.Entry<String, String> params : activeRule.getParams().entrySet()) {
        builder.setParam(params.getKey(), params.getValue());
      }
    }

    if (activeRule.getImpacts() != null) {
      for (Map.Entry<SoftwareQuality, Severity> impact : activeRule.getImpacts().entrySet()) {
        builder.setImpact(impact.getKey(), impact.getValue());
      }
    }

    return builder.build();
  }
}
