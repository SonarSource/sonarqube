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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IssueNormalizer extends BaseNormalizer<IssueDto, String> {

  public IssueNormalizer(DbClient db) {
    super(IndexDefinition.ISSUES, db);
  }

  public static final class IssueField extends Indexable {

    public static final IndexField KEY = addSortable(IndexField.Type.STRING, "key");

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
    Map<String, Object> upsert = new HashMap<String, Object>();

    update.put(IssueField.KEY.field(), dto.getKey());

    return ImmutableList.of(
      new UpdateRequest()
        .id(dto.getKey().toString())
        .doc(update)
        .upsert(update));
  }
}
