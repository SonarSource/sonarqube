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
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.persistence.Dto;
import org.sonar.server.db.DbClient;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public abstract class BaseNormalizer<E extends Dto<K>, K extends Serializable> {

  private static final Logger LOG = LoggerFactory.getLogger(BaseNormalizer.class);

  protected final DbClient db;

  protected BaseNormalizer(DbClient db) {
    this.db = db;
  }

  protected DbClient db() {
    return db;
  }

  public boolean canNormalize(Class<?> objectClass, Class<?> keyClass) {
    try {
      return this.getClass().getMethod("normalize", objectClass, keyClass) != null;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  public UpdateRequest normalizeOther(Object object, Object key) {
    try {
      return (UpdateRequest) this.getClass()
        .getMethod("normalize", object.getClass(), key.getClass())
        .invoke(this, object, key);
    } catch (Exception e) {
      throw new IllegalStateException("Could not invoke Normalizer Method", e);
    }
  }

  public abstract UpdateRequest normalize(K key);

  public abstract UpdateRequest normalize(E dto);

  protected void indexField(String field, Object value, XContentBuilder document) {
    try {
      document.field(field, value);
    } catch (IOException e) {
      LOG.error("Could not set {} to {} in ESDocument", field, value);
    }
  }


  protected UpdateRequest nestedUpsert(String field, String key, Map<String, Object> item) {
    return new UpdateRequest()
      .script("if(ctx._source.containsKey(\""+field+"\")){for (int i = 0; i < ctx._source." + field + ".size(); i++){" +
        "if(ctx._source." + field + "[i]._id == update_id){ ctx._source." + field + "[i] = update_doc;  update_done = true;}}" +
        "if(!update_done){ ctx._source." + field + "  += update_doc; }\n} else {ctx._source." + field + "  = [update_doc];}")
      .addScriptParam("update_id", key)
      .addScriptParam("update_doc", item)
      .addScriptParam("update_done", false);

  }
}
