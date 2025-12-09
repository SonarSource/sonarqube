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
package org.sonar.server.rule.registration;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.core.rule.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.SeverityUtil;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.qualityprofile.QProfileImpactSeverityMapper;

import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * Creates the initial impacts on active_rules, based on the rules. This class is necessary only up to 10 LTA version and can be removed
 * later
 */
public class ActiveRulesImpactInitializer {
  private static final String ACTIVE_RULE_IMPACT_INITIAL_POPULATION_DONE = "activeRules.impacts.populated";

  private final InternalProperties internalProperties;
  private final DbClient dbClient;

  ActiveRulesImpactInitializer(InternalProperties internalProperties, DbClient dbClient) {
    this.internalProperties = internalProperties;
    this.dbClient = dbClient;
  }

  public void createImpactsOnActiveRules(RulesRegistrationContext context, RulesDefinition.Repository repoDef, DbSession dbSession) {

    if (Boolean.parseBoolean(internalProperties.read(ACTIVE_RULE_IMPACT_INITIAL_POPULATION_DONE).orElse("false"))) {
      return;
    }

    Map<String, RuleDto> rules = new HashMap<>(repoDef.rules().size());

    for (RulesDefinition.Rule ruleDef : repoDef.rules()) {
      context.getDbRuleFor(ruleDef).ifPresent(ruleDto -> rules.put(ruleDto.getUuid(), ruleDto));
    }

    context.getAllModified().forEach(r -> rules.put(r.getUuid(), r));

    List<ActiveRuleDto> activeRules = dbClient.activeRuleDao().selectByRepository(dbSession, repoDef.key(), repoDef.language());

    for (ActiveRuleDto activeRuleDto : activeRules) {
      RuleDto rule = rules.get(activeRuleDto.getRuleUuid());
      if (rule == null || rule.getEnumType() == RuleType.SECURITY_HOTSPOT) {
        continue;
      }

      Map<SoftwareQuality, Severity> impacts = toImpactMap(rule.getDefaultImpacts());

      if (!activeRuleDto.getSeverity().equals(rule.getSeverity())) {
        impacts = QProfileImpactSeverityMapper.mapImpactSeverities(SeverityUtil.getSeverityFromOrdinal(activeRuleDto.getSeverity()), impacts, rule.getEnumType());
      }
      activeRuleDto.setImpacts(impacts);
      dbClient.activeRuleDao().update(dbSession, activeRuleDto);
    }

  }

  private static Map<SoftwareQuality, Severity> toImpactMap(Collection<ImpactDto> impacts) {
    return impacts.stream()
      .collect(toUnmodifiableMap(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity));
  }

  public void markInitialPopulationDone() {
    internalProperties.write(ACTIVE_RULE_IMPACT_INITIAL_POPULATION_DONE, "true");
  }
}
