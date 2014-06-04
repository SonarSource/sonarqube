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
package org.sonar.server.log.index;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.core.log.db.LogDto;
import org.sonar.core.log.db.LogKey;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.search.BaseNormalizer;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.IndexField;
import org.sonar.server.search.Indexable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @since 4.4
 */
public class LogNormalizer extends BaseNormalizer<LogDto, LogKey> {


  public static final class LogFields extends Indexable {

    public final static IndexField KEY = addSortableAndSearchable(IndexField.Type.STRING, "key");
    public final static IndexField TIME = addSortable(IndexField.Type.DATE, "time");
    public final static IndexField EXECUTION = add(IndexField.Type.NUMERIC, "executionTime");
    public final static IndexField AUTHOR = addSearchable(IndexField.Type.STRING, "executionTime");

    public static Set<IndexField> ALL_FIELDS = getAllFields();

    private static Set<IndexField> getAllFields() {
      Set<IndexField> fields = new HashSet<IndexField>();
      for (Field classField : LogFields.class.getDeclaredFields()) {
        if (Modifier.isFinal(classField.getModifiers()) && Modifier.isStatic(classField.getModifiers())) {
          try {
            fields.add(IndexField.class.cast(classField.get(null)));
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          }
        }
      }
      return fields;
    }
  }

  public LogNormalizer(DbClient db) {
    super(IndexDefinition.LOG, db);
  }

  @Override
  public List<UpdateRequest> normalize(Object object, LogKey key) {
    throw new IllegalStateException("Normalizer is not yet implemented");
  }

  @Override
  public List<UpdateRequest> normalize(LogKey logKey) {
    DbSession dbSession = db.openSession(false);
    List<UpdateRequest> requests = new ArrayList<UpdateRequest>();
    try {
      requests.addAll(normalize(db.logDao().getNullableByKey(dbSession, logKey)));
    } finally {
      dbSession.close();
    }
    return requests;
  }

  @Override
  public List<UpdateRequest> normalize(LogDto dto) {

    Map<String, Object> logDoc = new HashMap<String, Object>();
    logDoc.put("_id", dto.getKey());
    logDoc.put(LogFields.KEY.field(), dto.getKey());
    logDoc.put(LogFields.AUTHOR.field(), dto.getAuthor());
    logDoc.put(LogFields.EXECUTION.field(), dto.getExecutionTime());
    logDoc.put(LogFields.TIME.field(), dto.getTime());



   /* Creating updateRequest */
    return ImmutableList.of(new UpdateRequest()
      .id(dto.getKey().toString())
      .doc(logDoc)
      .upsert(logDoc));
  }
}
