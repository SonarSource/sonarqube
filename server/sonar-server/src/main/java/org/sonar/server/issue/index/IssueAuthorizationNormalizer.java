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
import org.sonar.core.issue.db.IssueAuthorizationDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.search.BaseNormalizer;
import org.sonar.server.search.IndexField;
import org.sonar.server.search.Indexable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IssueAuthorizationNormalizer extends BaseNormalizer<IssueAuthorizationDto, String> {

  public IssueAuthorizationNormalizer(DbClient db) {
    super(db);
  }

  public static final class IssueAuthorizationField extends Indexable {

    public static final IndexField PROJECT = add(IndexField.Type.STRING, "project");
    public static final IndexField PERMISSION = add(IndexField.Type.STRING, "permission");
    public static final IndexField GROUPS = add(IndexField.Type.STRING, "groups");
    public static final IndexField USERS = add(IndexField.Type.STRING, "users");
    public static final IndexField UPDATED_AT = add(IndexField.Type.DATE, BaseNormalizer.UPDATED_AT_FIELD);

    public static final Set<IndexField> ALL_FIELDS = ImmutableSet.of(PROJECT, PERMISSION, GROUPS, USERS, UPDATED_AT);
  }

  @Override
  public List<UpdateRequest> normalize(IssueAuthorizationDto dto) {
    Map<String, Object> update = new HashMap<String, Object>();

    Preconditions.checkNotNull(dto.getProjectUuid(), "Project uuid is null");

    update.put(IssueAuthorizationField.PROJECT.field(), dto.getProjectUuid());
    update.put(IssueAuthorizationField.PERMISSION.field(), dto.getPermission());
    update.put(IssueAuthorizationField.USERS.field(), dto.getUsers());
    update.put(IssueAuthorizationField.GROUPS.field(), dto.getGroups());
    update.put(IssueAuthorizationField.UPDATED_AT.field(), dto.getUpdatedAt());

    /** Upsert elements */
    Map<String, Object> upsert = getUpsertFor(IssueAuthorizationField.ALL_FIELDS, update);
    upsert.put(IssueAuthorizationField.PROJECT.field(), dto.getKey());

    return ImmutableList.of(
      new UpdateRequest()
        .id(dto.getKey())
        .doc(update)
        .upsert(upsert));
  }
}
