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
package org.sonar.server.issue.index;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDto;
import org.sonarsource.compliancereports.reports.ComplianceCategoryRules;
import org.sonarsource.compliancereports.reports.MetadataRules;
import org.sonarsource.compliancereports.reports.ReportKey;

/**
 * Adds compliance standards filters to an IssueQuery Builder
 */
@ServerSide
public class IssueQueryComplianceStandardService {

  private final MetadataRules metadataRules;
  private final DbClient dbClient;

  public IssueQueryComplianceStandardService(MetadataRules metadataRules, DbClient dbClient) {
    this.metadataRules = metadataRules;
    this.dbClient = dbClient;
  }

  public void addComplianceStandardFilters(DbSession session, IssueQuery.Builder builder,
    @Nullable Map<ReportKey, Set<String>> categoriesByStandard) {
    if (categoriesByStandard == null || categoriesByStandard.isEmpty()) {
      return;
    }

    Map<ReportKey, ComplianceCategoryRules> rulesByStandard = metadataRules.getRulesByStandard(categoriesByStandard);
    builder.complianceCategoryRules(getRuleIds(session, rulesByStandard));
  }

  private Set<String> getRuleIds(DbSession session, Map<ReportKey, ComplianceCategoryRules> rulesByStandard) {
    Set<String> ruleIds = null;

    for(Map.Entry<ReportKey, ComplianceCategoryRules> e : rulesByStandard.entrySet()) {
      ComplianceCategoryRules rules = e.getValue();
      Set<RuleKey> ruleKeys = rules.allRepoRuleKeys().stream().map(r -> RuleKey.of(r.repository(), r.rule())).collect(Collectors.toSet());

      Set<String> ids = Stream.concat(
        dbClient.ruleDao().selectByKeys(session, ruleKeys).stream(),
        dbClient.ruleDao().selectByRuleKeys(session, rules.allRuleKeys()).stream()
      ).map(RuleDto::getUuid).collect(Collectors.toSet());

      // standard doesn't exist or it doesn't have any rules associated to it
      if (ids.isEmpty()) {
        return Set.of("non-existing-uuid");
      }
      if(ruleIds == null) {
        ruleIds = ids;
      } else {
        ruleIds.retainAll(ids);
      }
    }

    return ruleIds;
  }
}
