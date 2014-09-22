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
package org.sonar.server.activity.index;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.action.support.replication.ReplicationType;
import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.core.activity.db.ActivityDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.search.BaseNormalizer;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.IndexField;
import org.sonar.server.search.Indexable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @since 4.4
 */
public class ActivityNormalizer extends BaseNormalizer<ActivityDto, String> {


  public static final class LogFields extends Indexable {

    public static final IndexField KEY = add(IndexField.Type.STRING, "key");
    public static final IndexField TYPE = addSortable(IndexField.Type.STRING, "type");
    public static final IndexField ACTION = addSortable(IndexField.Type.STRING, "action");
    public static final IndexField CREATED_AT = addSortable(IndexField.Type.DATE, "createdAt");
    public static final IndexField UPDATED_AT = addSortable(IndexField.Type.DATE, BaseNormalizer.UPDATED_AT_FIELD);
    public static final IndexField LOGIN = addSearchable(IndexField.Type.STRING, "login");
    public static final IndexField DETAILS = addSearchable(IndexField.Type.OBJECT, "details");
    public static final IndexField MESSAGE = addSearchable(IndexField.Type.STRING, "message");

    public static Set<IndexField> ALL_FIELDS = getAllFields();

    private static final Set<IndexField> getAllFields() {
      Set<IndexField> fields = new HashSet<IndexField>();
      for (Field classField : LogFields.class.getDeclaredFields()) {
        if (Modifier.isFinal(classField.getModifiers()) && Modifier.isStatic(classField.getModifiers())) {
          try {
            fields.add(IndexField.class.cast(classField.get(null)));
          } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not access Field '" + classField.getName() + "'", e);
          }
        }
      }
      return fields;
    }
  }

  public ActivityNormalizer(DbClient db) {
    super(IndexDefinition.LOG, db);
  }

  @Override
  public List<UpdateRequest> normalize(ActivityDto dto) {

    Map<String, Object> logDoc = new HashMap<String, Object>();
    logDoc.put(LogFields.KEY.field(), dto.getKey());
    logDoc.put(LogFields.TYPE.field(), dto.getType());
    logDoc.put(LogFields.ACTION.field(), dto.getAction());
    logDoc.put(LogFields.LOGIN.field(), dto.getAuthor());
    logDoc.put(LogFields.MESSAGE.field(), dto.getMessage());
    logDoc.put(LogFields.CREATED_AT.field(), dto.getCreatedAt());
    logDoc.put(LogFields.UPDATED_AT.field(), dto.getUpdatedAt());

    logDoc.put(LogFields.DETAILS.field(), KeyValueFormat.parse(dto.getData()));

   /* Creating updateRequest */
    return ImmutableList.of(new UpdateRequest()
      .id(dto.getKey())
      .replicationType(ReplicationType.ASYNC)
      .doc(logDoc)
      .upsert(logDoc));
  }
}
