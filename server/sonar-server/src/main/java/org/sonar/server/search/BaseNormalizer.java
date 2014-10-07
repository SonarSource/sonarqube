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
package org.sonar.server.search;

import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.core.persistence.Dto;
import org.sonar.server.db.DbClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseNormalizer<DTO extends Dto<KEY>, KEY extends Serializable> {

  public static final String UPDATED_AT_FIELD = "updatedAt";

  protected final DbClient db;

  protected BaseNormalizer(DbClient db) {
    this.db = db;
  }

  protected Map<String, Object> getUpsertFor(Set<IndexField> fields, Map<String, Object> update) {
    Map<String, Object> upsert = new HashMap<String, Object>(update);
    for (IndexField field : fields) {
      if (!upsert.containsKey(field.field())) {
        if (field.type().equals(IndexField.Type.OBJECT)) {
          upsert.put(field.field(), new ArrayList<String>());
        } else {
          upsert.put(field.field(), null);
        }
      }
    }
    return upsert;
  }

  public List<UpdateRequest> deleteNested(Object object, KEY key) {
    throw new IllegalStateException("Nested Delete not implemented in current normalizer!");
  }

  public List<UpdateRequest> normalizeNested(Object object, KEY key) {
    throw new IllegalStateException("Nested Normalize not implemented in current normalizer!");
  }

  public abstract List<UpdateRequest> normalize(DTO dto);
}
