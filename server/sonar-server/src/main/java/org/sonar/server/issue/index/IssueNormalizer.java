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
package org.sonar.server.issue.index;

import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.core.persistence.Dto;
import org.sonar.server.db.DbClient;
import org.sonar.server.search.BaseNormalizer;
import org.sonar.server.search.IndexField;
import org.sonar.server.search.Indexable;

import java.util.List;

public class IssueNormalizer extends BaseNormalizer {

  public IssueNormalizer(DbClient db) {
    super(db);
  }

  @Override
  public List<UpdateRequest> normalize(Dto dto) {
    throw new UnsupportedOperationException();
  }

  public static final class IssueField extends Indexable {

    public static final IndexField KEY = addSortable(IndexField.Type.STRING, "key");
    public static final IndexField CREATED_AT = add(IndexField.Type.DATE, "createdAt");
    public static final IndexField UPDATED_AT = add(IndexField.Type.DATE, "updatedAt");

    public static final IndexField PROJECT = addSortable(IndexField.Type.STRING, "project");
    public static final IndexField COMPONENT = add(IndexField.Type.STRING, "component");
    public static final IndexField MODULE = add(IndexField.Type.STRING, "module");
    public static final IndexField MODULE_PATH = add(IndexField.Type.UUID_PATH, "modulePath");

    public static final IndexField ACTION_PLAN = add(IndexField.Type.STRING, "actionPlan");
    public static final IndexField ASSIGNEE = addSortable(IndexField.Type.STRING, "assignee");
    public static final IndexField ATTRIBUTES = add(IndexField.Type.STRING, "attributes");
    public static final IndexField AUTHOR_LOGIN = add(IndexField.Type.STRING, "authorLogin");
    public static final IndexField DEBT = add(IndexField.Type.LONG, "debt");
    public static final IndexField EFFORT = add(IndexField.Type.DOUBLE, "effort");
    public static final IndexField ISSUE_CREATED_AT = addSortable(IndexField.Type.DATE, "issueCreatedAt");
    public static final IndexField ISSUE_UPDATED_AT = addSortable(IndexField.Type.DATE, "issueUpdatedAt");
    public static final IndexField ISSUE_CLOSE_DATE = addSortable(IndexField.Type.DATE, "issueClosedAt");
    public static final IndexField LINE = addSortable(IndexField.Type.INTEGER, "line");
    public static final IndexField MESSAGE = add(IndexField.Type.STRING, "message");
    public static final IndexField RESOLUTION = add(IndexField.Type.STRING, "resolution");
    public static final IndexField REPORTER = add(IndexField.Type.STRING, "reporter");
    public static final IndexField STATUS = addSortable(IndexField.Type.STRING, "status");
    public static final IndexField SEVERITY = add(IndexField.Type.STRING, "severity");
    public static final IndexField SEVERITY_VALUE = addSortable(IndexField.Type.INTEGER, "severityValue");
    public static final IndexField LANGUAGE = add(IndexField.Type.STRING, "language");
    public static final IndexField RULE_KEY = add(IndexField.Type.STRING, "ruleKey");
    public static final IndexField FILE_PATH = addSortable(IndexField.Type.STRING, "filePath");
  }

}
