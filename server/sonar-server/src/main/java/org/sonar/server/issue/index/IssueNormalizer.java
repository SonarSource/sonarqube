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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.api.rule.Severity;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.search.BaseNormalizer;
import org.sonar.server.search.IndexField;
import org.sonar.server.search.Indexable;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;

public class IssueNormalizer extends BaseNormalizer<IssueDto, String> {

  public IssueNormalizer(DbClient db) {
    super(db);
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
    public static final IndexField DEBT = add(IndexField.Type.DOUBLE, "debt");
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

    public static final Set<IndexField> ALL_FIELDS = ImmutableSet.of(KEY, CREATED_AT, UPDATED_AT, PROJECT, COMPONENT,
      MODULE, MODULE_PATH, ACTION_PLAN, ASSIGNEE, ATTRIBUTES, AUTHOR_LOGIN, DEBT, EFFORT, ISSUE_CREATED_AT,
      ISSUE_UPDATED_AT, ISSUE_CLOSE_DATE, LINE, MESSAGE, RESOLUTION, REPORTER, STATUS, SEVERITY, SEVERITY_VALUE,
      LANGUAGE, RULE_KEY, FILE_PATH);
  }

  @Override
  public List<UpdateRequest> normalize(IssueDto dto) {
    Map<String, Object> update = newHashMap();

    Preconditions.checkNotNull(dto.getProjectUuid(), "Project uuid is null on issue %s", dto.getKey());
    Preconditions.checkNotNull(dto.getComponentUuid(), "Component uuid is null on issue %s", dto.getKey());

    update.put("_parent", dto.getProjectUuid());
    update.put(IssueField.KEY.field(), dto.getKey());
    update.put(IssueField.UPDATED_AT.field(), dto.getUpdatedAt());
    update.put(IssueField.CREATED_AT.field(), dto.getCreatedAt());

    update.put(IssueField.PROJECT.field(), dto.getProjectUuid());
    update.put(IssueField.COMPONENT.field(), dto.getComponentUuid());
    update.put(IssueField.MODULE.field(), dto.getModuleUuid());
    update.put(IssueField.MODULE_PATH.field(), dto.getModuleUuidPath());

    update.put(IssueField.ACTION_PLAN.field(), dto.getActionPlanKey());
    update.put(IssueField.ATTRIBUTES.field(), dto.getIssueAttributes());
    update.put(IssueField.ASSIGNEE.field(), dto.getAssignee());
    update.put(IssueField.AUTHOR_LOGIN.field(), dto.getAuthorLogin());
    update.put(IssueField.ISSUE_CLOSE_DATE.field(), dto.getIssueCloseDate());
    update.put(IssueField.ISSUE_CREATED_AT.field(), dto.getIssueCreationDate());
    update.put(IssueField.ISSUE_UPDATED_AT.field(), dto.getIssueUpdateDate());
    update.put(IssueField.EFFORT.field(), dto.getEffortToFix());
    update.put(IssueField.RESOLUTION.field(), dto.getResolution());
    update.put(IssueField.LINE.field(), dto.getLine());
    update.put(IssueField.MESSAGE.field(), dto.getMessage());
    update.put(IssueField.REPORTER.field(), dto.getReporter());
    update.put(IssueField.STATUS.field(), dto.getStatus());
    update.put(IssueField.SEVERITY.field(), dto.getSeverity());
    update.put(IssueField.SEVERITY_VALUE.field(), Severity.ALL.indexOf(dto.getSeverity()));
    update.put(IssueField.DEBT.field(), dto.getDebt());
    update.put(IssueField.LANGUAGE.field(), dto.getLanguage());
    update.put(IssueField.RULE_KEY.field(), dto.getRuleKey().toString());
    update.put(IssueField.FILE_PATH.field(), dto.getFilePath());

    /** Upsert elements */
    Map<String, Object> upsert = getUpsertFor(IssueField.ALL_FIELDS, update);
    upsert.put(IssueField.KEY.field(), dto.getKey());

    return ImmutableList.of(
      new UpdateRequest()
        .id(dto.getKey())
        .routing(dto.getProjectUuid())
        .parent(dto.getProjectUuid())
        .doc(update)
        .upsert(upsert));
  }
}
