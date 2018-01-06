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
package org.sonar.server.computation.task.projectanalysis.issue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueMapper;
import org.sonar.server.computation.task.projectanalysis.qualityprofile.ActiveRulesHolder;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class ComponentIssuesLoader {
  private final DbClient dbClient;
  private final RuleRepository ruleRepository;
  private final ActiveRulesHolder activeRulesHolder;

  public ComponentIssuesLoader(DbClient dbClient, RuleRepository ruleRepository, ActiveRulesHolder activeRulesHolder) {
    this.activeRulesHolder = activeRulesHolder;
    this.dbClient = dbClient;
    this.ruleRepository = ruleRepository;
  }

  public List<DefaultIssue> loadForComponentUuid(String componentUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return loadForComponentUuid(componentUuid, dbSession);
    }
  }

  public List<DefaultIssue> loadForComponentUuidWithChanges(String componentUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<DefaultIssue> result = loadForComponentUuid(componentUuid, dbSession);

      Map<String, List<IssueChangeDto>> changeDtoByIssueKey = dbClient.issueChangeDao()
        .selectByIssueKeys(dbSession, result.stream().map(DefaultIssue::key).collect(toList()))
        .stream()
        .collect(groupingBy(IssueChangeDto::getIssueKey));

      return result
        .stream()
        .peek(i -> setChanges(changeDtoByIssueKey, i))
        .collect(toList());
    }
  }

  private List<DefaultIssue> loadForComponentUuid(String componentUuid, DbSession dbSession) {
    List<DefaultIssue> result = new ArrayList<>();
    dbSession.getMapper(IssueMapper.class).scrollNonClosedByComponentUuid(componentUuid, resultContext -> {
      DefaultIssue issue = (resultContext.getResultObject()).toDefaultIssue();

      // TODO this field should be set outside this class
      if (!isActive(issue.ruleKey()) || ruleRepository.getByKey(issue.ruleKey()).getStatus() == RuleStatus.REMOVED) {
        issue.setOnDisabledRule(true);
        // TODO to be improved, why setOnDisabledRule(true) is not enough ?
        issue.setBeingClosed(true);
      }
      // FIXME
      issue.setSelectedAt(System.currentTimeMillis());
      result.add(issue);
    });
    return result;
  }

  public static void setChanges(Map<String, List<IssueChangeDto>> changeDtoByIssueKey, DefaultIssue i) {
    changeDtoByIssueKey.computeIfAbsent(i.key(), k -> emptyList()).forEach(c -> {
      switch (c.getChangeType()) {
        case IssueChangeDto.TYPE_FIELD_CHANGE:
          i.addChange(c.toFieldDiffs());
          break;
        case IssueChangeDto.TYPE_COMMENT:
          i.addComment(c.toComment());
          break;
        default:
          throw new IllegalStateException("Unknow change type: " + c.getChangeType());
      }
    });
  }

  private boolean isActive(RuleKey ruleKey) {
    return activeRulesHolder.get(ruleKey).isPresent();
  }
}
