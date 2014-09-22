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

import com.google.common.collect.ImmutableList;
import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.search.BaseNormalizer;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.IndexField;
import org.sonar.server.search.Indexable;

import java.lang.reflect.Field;
import java.util.*;

public class IssueNormalizer extends BaseNormalizer<IssueDto, String> {

  public IssueNormalizer(DbClient db) {
    super(IndexDefinition.ISSUES, db);
  }

  public static final class IssueField extends Indexable {

    public static final IndexField KEY = addSortable(IndexField.Type.STRING, "key");
    public static final IndexField CREATED_AT = add(IndexField.Type.DATE, "createdAt");
    public static final IndexField UPDATED_AT = add(IndexField.Type.DATE, "updatedAt");

    public static final IndexField ACTION_PLAN = add(IndexField.Type.STRING, "actionPlan");
    public static final IndexField ASSIGNEE = add(IndexField.Type.STRING, "assignee");
    public static final IndexField ATTRIBUTE = add(IndexField.Type.OBJECT, "attributes");
    public static final IndexField AUTHOR_LOGIN = add(IndexField.Type.STRING, "authorLogin");
    public static final IndexField COMPONENT = add(IndexField.Type.STRING, "component");
    public static final IndexField DEBT = add(IndexField.Type.NUMERIC, "debt");
    public static final IndexField EFFORT = add(IndexField.Type.NUMERIC, "effort");
    public static final IndexField ISSUE_CREATED_AT = add(IndexField.Type.DATE, "issueCreatedAt");
    public static final IndexField ISSUE_UPDATED_AT = add(IndexField.Type.DATE, "issueUpdatedAt");
    public static final IndexField ISSUE_CLOSE_DATE = add(IndexField.Type.DATE, "issueClosedAt");
    public static final IndexField LINE = add(IndexField.Type.NUMERIC, "line");
    public static final IndexField MESSAGE = add(IndexField.Type.STRING, "message");
    public static final IndexField PROJECT = add(IndexField.Type.STRING, "project");
    public static final IndexField RESOLUTION = add(IndexField.Type.STRING, "resolution");
    public static final IndexField REPORTER = add(IndexField.Type.STRING, "reporter");
    public static final IndexField STATUS = add(IndexField.Type.STRING, "status");
    public static final IndexField SEVERITY = add(IndexField.Type.STRING, "severity");
    public static final IndexField LANGUAGE = add(IndexField.Type.STRING, "language");
    public static final IndexField RULE_KEY = addSearchable(IndexField.Type.STRING, "ruleKey");

    public static final Set<IndexField> ALL_FIELDS = getAllFields();

    private static final Set<IndexField> getAllFields() {
      Set<IndexField> fields = new HashSet<IndexField>();
      for (Field classField : IssueField.class.getDeclaredFields()) {
        if (classField.getType().isAssignableFrom(IndexField.class)) {
          try {
            fields.add(IndexField.class.cast(classField.get(null)));
          } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not access Field '" + classField.getName() + "'", e);
          }
        }
      }
      return fields;
    }

    public static final IndexField of(String fieldName) {
      for (IndexField field : ALL_FIELDS) {
        if (field.field().equals(fieldName)) {
          return field;
        }
      }
      throw new IllegalStateException("Could not find an IndexField for '" + fieldName + "'");
    }
  }

  @Override
  public List<UpdateRequest> normalize(IssueDto dto) {
    Map<String, Object> update = new HashMap<String, Object>();

    update.put("_parent", dto.getRootComponentKey());
    update.put(IssueField.KEY.field(), dto.getKey());
    update.put(IssueField.UPDATED_AT.field(), dto.getUpdatedAt());
    update.put(IssueField.CREATED_AT.field(), dto.getCreatedAt());

    update.put(IssueField.ACTION_PLAN.field(), dto.getActionPlanKey());
    update.put(IssueField.ASSIGNEE.field(), dto.getAssignee());
    update.put(IssueField.AUTHOR_LOGIN.field(), dto.getAuthorLogin());
    update.put(IssueField.ISSUE_CLOSE_DATE.field(), dto.getIssueCloseDate());
    update.put(IssueField.PROJECT.field(), dto.getRootComponentKey());
    update.put(IssueField.COMPONENT.field(), dto.getComponentKey());
    update.put(IssueField.ISSUE_CREATED_AT.field(), dto.getIssueCreationDate());
    update.put(IssueField.ISSUE_UPDATED_AT.field(), dto.getIssueUpdateDate());
    update.put(IssueField.EFFORT.field(), dto.getEffortToFix());
    update.put(IssueField.RESOLUTION.field(), dto.getResolution());
    update.put(IssueField.LINE.field(), dto.getLine());
    update.put(IssueField.MESSAGE.field(), dto.getMessage());
    update.put(IssueField.REPORTER.field(), dto.getReporter());
    update.put(IssueField.STATUS.field(), dto.getStatus());
    update.put(IssueField.SEVERITY.field(), dto.getSeverity());
    update.put(IssueField.DEBT.field(), dto.getDebt());
    update.put(IssueField.LANGUAGE.field(), dto.getLanguage());
    update.put(IssueField.RULE_KEY.field(), dto.getRuleKey().toString());

    // TODO Not yet normalized
    // IssueDoc issueDoc = new IssueDoc(null);
    // issueDoc.isNew();
    // issueDoc.comments();
    // issueDoc.attributes();

    /** Upsert elements */
    Map<String, Object> upsert = getUpsertFor(IssueField.ALL_FIELDS, update);
    upsert.put(IssueField.KEY.field(), dto.getKey().toString());

    return ImmutableList.of(
      new UpdateRequest()
        .id(dto.getKey().toString())
        .routing(dto.getRootComponentKey())
        .parent(dto.getRootComponentKey())
        .doc(update)
        .upsert(upsert));
  }
}
