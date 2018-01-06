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
package org.sonar.server.issue;

import java.util.Collection;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
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

  public ServerIssueStorage(System2 system2, RuleFinder ruleFinder, DbClient dbClient, IssueIndexer indexer) {
    super(system2, dbClient, ruleFinder);
    this.indexer = indexer;
  }

  @Override
  protected IssueDto doInsert(DbSession session, long now, DefaultIssue issue) {
    ComponentDto component = component(session, issue);
    ComponentDto project = project(session, issue);
    int ruleId = rule(issue).getId();
    IssueDto dto = IssueDto.toDtoForServerInsert(issue, component, project, ruleId, now);

    getDbClient().issueDao().insert(session, dto);
    return dto;
  }

  @Override
  protected IssueDto doUpdate(DbSession session, long now, DefaultIssue issue) {
    IssueDto dto = IssueDto.toDtoForUpdate(issue, now);
    getDbClient().issueDao().update(session, dto);
    return dto;
  }

  @Override
  protected void doAfterSave(DbSession dbSession, Collection<IssueDto> issues) {
    indexer.commitAndIndexIssues(dbSession, issues);
  }

  protected ComponentDto component(DbSession session, DefaultIssue issue) {
    return getDbClient().componentDao().selectOrFailByUuid(session, issue.componentUuid());
  }

  protected ComponentDto project(DbSession session, DefaultIssue issue) {
    return getDbClient().componentDao().selectOrFailByUuid(session, issue.projectUuid());
  }
}
