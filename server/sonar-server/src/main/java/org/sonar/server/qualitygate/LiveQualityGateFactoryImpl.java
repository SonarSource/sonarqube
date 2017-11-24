/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualitygate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.elasticsearch.action.search.SearchResponse;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.rule.index.RuleIndex;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Collections.singletonList;
import static org.sonar.core.util.stream.MoreCollectors.toSet;

public class LiveQualityGateFactoryImpl implements LiveQualityGateFactory {

  private final IssueIndex issueIndex;
  private final System2 system2;

  public LiveQualityGateFactoryImpl(IssueIndex issueIndex, System2 system2) {
    this.issueIndex = issueIndex;
    this.system2 = system2;
  }

  @Override
  public EvaluatedQualityGate buildForShortLivedBranch(ComponentDto componentDto) {
    return createQualityGate(componentDto, issueIndex);
  }

  private EvaluatedQualityGate createQualityGate(ComponentDto project, IssueIndex issueIndex) {
    SearchResponse searchResponse = issueIndex.search(IssueQuery.builder()
            .projectUuids(singletonList(project.getMainBranchProjectUuid()))
            .branchUuid(project.uuid())
            .mainBranch(false)
            .resolved(false)
            .checkAuthorization(false)
            .build(),
        new SearchOptions().addFacets(RuleIndex.FACET_TYPES));
    LinkedHashMap<String, Long> typeFacet = new Facets(searchResponse, system2.getDefaultTimeZone())
        .get(RuleIndex.FACET_TYPES);

    EvaluatedQualityGate.Builder builder = EvaluatedQualityGate.newBuilder();
    Set<Condition> conditions = ShortLivingBranchQualityGate.CONDITIONS.stream()
        .map(c -> {
          long measure = getMeasure(typeFacet, c);
          EvaluatedCondition.EvaluationStatus status = measure > 0 ? EvaluatedCondition.EvaluationStatus.ERROR : EvaluatedCondition.EvaluationStatus.OK;
          Condition condition = new Condition(c.getMetricKey(), toOperator(c), c.getErrorThreshold(), c.getWarnThreshold(), c.isOnLeak());
          builder.addCondition(condition, status, valueOf(measure));
          return condition;
        })
        .collect(toSet(ShortLivingBranchQualityGate.CONDITIONS.size()));
    builder
        .setQualityGate(
            new org.sonar.server.qualitygate.QualityGate(
                valueOf(ShortLivingBranchQualityGate.ID),
                ShortLivingBranchQualityGate.NAME,
                conditions))
        .setStatus(qgStatusFrom(builder.getEvaluatedConditions()));

    return builder.build();
  }

  private static Condition.Operator toOperator(ShortLivingBranchQualityGate.Condition c) {
    String operator = c.getOperator();
    switch (operator) {
      case QualityGateConditionDto.OPERATOR_GREATER_THAN:
        return Condition.Operator.GREATER_THAN;
      case QualityGateConditionDto.OPERATOR_LESS_THAN:
        return Condition.Operator.LESS_THAN;
      case QualityGateConditionDto.OPERATOR_EQUALS:
        return Condition.Operator.EQUALS;
      case QualityGateConditionDto.OPERATOR_NOT_EQUALS:
        return Condition.Operator.NOT_EQUALS;
      default:
        throw new IllegalArgumentException(format("Unsupported Condition operator '%s'", operator));
    }
  }

  private static EvaluatedQualityGate.Status qgStatusFrom(Set<EvaluatedCondition> conditions) {
    if (conditions.stream().anyMatch(c -> c.getStatus() == EvaluatedCondition.EvaluationStatus.ERROR)) {
      return EvaluatedQualityGate.Status.ERROR;
    }
    return EvaluatedQualityGate.Status.OK;
  }

  private static long getMeasure(LinkedHashMap<String, Long> typeFacet, ShortLivingBranchQualityGate.Condition c) {
    String metricKey = c.getMetricKey();
    switch (metricKey) {
      case CoreMetrics.BUGS_KEY:
        return getValueForRuleType(typeFacet, RuleType.BUG);
      case CoreMetrics.VULNERABILITIES_KEY:
        return getValueForRuleType(typeFacet, RuleType.VULNERABILITY);
      case CoreMetrics.CODE_SMELLS_KEY:
        return getValueForRuleType(typeFacet, RuleType.CODE_SMELL);
      default:
        throw new IllegalArgumentException(format("Unsupported metric key '%s' in hardcoded quality gate", metricKey));
    }
  }

  private static long getValueForRuleType(Map<String, Long> facet, RuleType ruleType) {
    Long res = facet.get(ruleType.name());
    if (res == null) {
      return 0L;
    }
    return res;
  }
}
