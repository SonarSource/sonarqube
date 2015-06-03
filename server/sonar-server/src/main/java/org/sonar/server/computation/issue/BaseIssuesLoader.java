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
package org.sonar.server.computation.issue;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.db.IssueMapper;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.db.DbClient;

/**
 * Loads all the project open issues from database, including manual issues.
 */
public class BaseIssuesLoader {

  private final TreeRootHolder treeRootHolder;
  private final DbClient dbClient;
  private final RuleCache ruleCache;

  public BaseIssuesLoader(TreeRootHolder treeRootHolder, DbClient dbClient, RuleCache ruleCache) {
    this.treeRootHolder = treeRootHolder;
    this.dbClient = dbClient;
    this.ruleCache = ruleCache;
  }

  public List<DefaultIssue> loadForComponentUuid(String componentUuid) {
    DbSession session = dbClient.openSession(false);
    final List<DefaultIssue> result = new ArrayList<>();
    try {
      Map<String, String> params = ImmutableMap.of("componentUuid", componentUuid);
      session.select(IssueMapper.class.getName() + ".selectOpenByComponentUuid", params, new ResultHandler() {
        @Override
        public void handleResult(ResultContext resultContext) {
          DefaultIssue issue = ((IssueDto) resultContext.getResultObject()).toDefaultIssue();
          issue.setOnDisabledRule(ruleCache.getNullable(issue.ruleKey()) == null);
          result.add(issue);
        }
      });
      return result;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Uuids of all the components that have open issues on this project.
   */
  public Set<String> loadComponentUuids() {
    DbSession session = dbClient.openSession(false);
    try {
      return dbClient.issueDao().selectComponentUuidsOfOpenIssuesForProjectUuid(session, treeRootHolder.getRoot().getUuid());
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
