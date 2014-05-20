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
package org.sonar.server.search.ws;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.picocontainer.Startable;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.search.BaseDoc;
import org.sonar.server.search.IndexUtils;
import org.sonar.server.search.QueryOptions;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Mapping of search documents (see BaseDoc) to WS JSON responses
 */
public abstract class BaseMapping implements ServerComponent, Startable {

  private final Multimap<String, String> indexFields = LinkedHashMultimap.create();
  private final Multimap<String, BaseMapping.Field> fields = LinkedHashMultimap.create();

  @Override
  public final void start() {
    doInit();
  }

  protected abstract void doInit();

  @Override
  public final void stop() {
    // do nothing
  }

  public Set<String> supportedFields() {
    return fields.keySet();
  }

  public QueryOptions newQueryOptions(SearchOptions options) {
    QueryOptions result = new QueryOptions();
    result.setPage(options.page(), options.pageSize());
    List<String> optionFields = options.fields();
    if (optionFields != null) {
      for (String optionField : optionFields) {
        result.addFieldsToReturn(this.indexFields.get(optionField));
      }
    }
    return result;
  }

  public void write(BaseDoc doc, JsonWriter json, @Nullable Collection<String> fieldsToReturn) {
    json.beginObject();
    json.prop("key", doc.keyField());
    if (fieldsToReturn == null || fieldsToReturn.isEmpty()) {
      // return all fields
      for (BaseMapping.Field field : fields.values()) {
        field.write(json, doc);
      }
    } else {
      for (String optionField : fieldsToReturn) {
        for (BaseMapping.Field field : fields.get(optionField)) {
          field.write(json, doc);
        }
      }
    }
    json.endObject();
  }

  protected BaseMapping addIndexField(String key, String indexKey) {
    indexFields.put(key, indexKey);
    fields.put(key, new BaseMapping.IndexField(key, indexKey));
    return this;
  }

  protected BaseMapping addIndexBooleanField(String key, String indexKey) {
    indexFields.put(key, indexKey);
    fields.put(key, new BaseMapping.IndexBooleanField(key, indexKey));
    return this;
  }

  protected BaseMapping addIndexDatetimeField(String key, String indexKey) {
    indexFields.put(key, indexKey);
    fields.put(key, new BaseMapping.IndexDatetimeField(key, indexKey));
    return this;
  }

  protected BaseMapping addIndexArrayField(String key, String indexKey) {
    indexFields.put(key, indexKey);
    fields.put(key, new BaseMapping.IndexArrayField(key, indexKey));
    return this;
  }

  protected BaseMapping addField(String key, BaseMapping.Field field) {
    fields.put(key, field);
    return this;
  }

  public static interface Field<D> {
    void write(JsonWriter json, D doc);
  }

  /**
   * String field
   */
  public static class IndexField implements Field<BaseDoc> {
    private final String key, indexKey;

    public IndexField(String key, String indexKey) {
      this.key = key;
      this.indexKey = indexKey;
    }

    @Override
    public void write(JsonWriter json, BaseDoc doc) {
      Object val = doc.getField(indexKey);
      json.prop(key, val != null ? val.toString() : null);
    }
  }

  public static class IndexBooleanField implements Field<BaseDoc> {
    private final String key, indexKey;

    public IndexBooleanField(String key, String indexKey) {
      this.key = key;
      this.indexKey = indexKey;
    }

    @Override
    public void write(JsonWriter json, BaseDoc doc) {
      Boolean val = doc.getField(indexKey);
      json.prop(key, val != null ? val.booleanValue() : null);
    }
  }

  public static class IndexArrayField implements Field<BaseDoc> {
    private final String key, indexKey;

    public IndexArrayField(String key, String indexKey) {
      this.key = key;
      this.indexKey = indexKey;
    }

    @Override
    public void write(JsonWriter json, BaseDoc doc) {
      Iterable<String> values = doc.getField(indexKey);
      json.name(key).beginArray().values(values).endArray();
    }
  }

  public static class IndexDatetimeField implements Field<BaseDoc> {
    private final String key, indexKey;

    public IndexDatetimeField(String key, String indexKey) {
      this.key = key;
      this.indexKey = indexKey;
    }

    @Override
    public void write(JsonWriter json, BaseDoc doc) {
      String val = doc.getField(indexKey);
      if (val != null) {
        json.propDateTime(key, IndexUtils.parseDateTime(val));
      }
    }
  }

}
