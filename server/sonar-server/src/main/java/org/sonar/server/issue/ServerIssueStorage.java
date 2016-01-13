/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.issue;

import org.sonar.api.rules.RuleFinder;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.issue.index.IssueIndexer;

/**
 * @since 3.6
 */
@ServerSide
public class ServerIssueStorage extends IssueStorage {

  private final IssueIndexer indexer;

  public ServerIssueStorage(RuleFinder ruleFinder, DbClient dbClient, IssueIndexer indexer) {
    super(dbClient, ruleFinder);
    this.indexer = indexer;
  }

  @Override
  protected void doInsert(DbSession session, long now, DefaultIssue issue) {
    ComponentDto component = component(session, issue);
    ComponentDto project = project(session, issue);
    int ruleId = rule(issue).getId();
    IssueDto dto = IssueDto.toDtoForServerInsert(issue, component, project, ruleId, now);

    getDbClient().issueDao().insert(session, dto);
  }

  @Override
  protected void doUpdate(DbSession session, long now, DefaultIssue issue) {
    IssueDto dto = IssueDto.toDtoForUpdate(issue, now);

    getDbClient().issueDao().update(session, dto);
  }

  @Override
  protected void doAfterSave() {
    indexer.index();
  }

  protected ComponentDto component(DbSession session, DefaultIssue issue) {
    return getDbClient().componentDao().selectOrFailByKey(session, issue.componentKey());
  }

  protected ComponentDto project(DbSession session, DefaultIssue issue) {
    return getDbClient().componentDao().selectOrFailByKey(session, issue.projectKey());
  }
}
