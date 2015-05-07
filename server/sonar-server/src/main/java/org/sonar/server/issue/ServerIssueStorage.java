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
package org.sonar.server.issue;

import org.sonar.api.ServerSide;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rules.RuleFinder;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueIndexer;

/**
 * @since 3.6
 */
@ServerSide
public class ServerIssueStorage extends IssueStorage {

  private final DbClient dbClient;
  private final IssueIndexer indexer;

  public ServerIssueStorage(MyBatis mybatis, RuleFinder ruleFinder, DbClient dbClient, IssueIndexer indexer) {
    super(mybatis, ruleFinder);
    this.dbClient = dbClient;
    this.indexer = indexer;
  }

  @Override
  protected void doInsert(DbSession session, long now, DefaultIssue issue) {
    ComponentDto component = component(session, issue);
    ComponentDto project = project(session, issue);
    int ruleId = rule(issue).getId();
    IssueDto dto = IssueDto.toDtoForServerInsert(issue, component, project, ruleId, now);

    dbClient.issueDao().insert(session, dto);
  }

  @Override
  protected void doUpdate(DbSession session, long now, DefaultIssue issue) {
    IssueDto dto = IssueDto.toDtoForUpdate(issue, now);

    dbClient.issueDao().update(session, dto);
  }

  @Override
  protected void doAfterSave() {
    indexer.index();
  }

  protected ComponentDto component(DbSession session, DefaultIssue issue) {
    return dbClient.componentDao().getByKey(session, issue.componentKey());
  }

  protected ComponentDto project(DbSession session, DefaultIssue issue) {
    return dbClient.componentDao().getByKey(session, issue.projectKey());
  }
}
