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
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.search.BaseDoc;
import org.sonar.server.search.IndexUtils;
import org.sonar.server.search.QueryOptions;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Mapping of search documents (see BaseDoc) to WS JSON responses
 */
public abstract class BaseMapping implements ServerComponent {

  private final Multimap<String, String> indexFields = LinkedHashMultimap.create();
  private final Multimap<String, BaseMapping.Field> fields = LinkedHashMultimap.create();

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

  public void write(BaseDoc doc, JsonWriter json) {
    write(doc, json, null);
  }

  public void write(BaseDoc doc, JsonWriter json, @Nullable SearchOptions options) {
    json.beginObject();
    json.prop("key", doc.keyField());
    if (options == null || options.fields() == null) {
      // return all fields
      for (BaseMapping.Field field : fields.values()) {
        field.write(json, doc);
      }
    } else {
      for (String optionField : options.fields()) {
        for (BaseMapping.Field field : fields.get(optionField)) {
          field.write(json, doc);
        }
      }
    }
    json.endObject();
  }

  protected BaseMapping addIndexStringField(String key, String indexKey) {
    return addField(key, new IndexStringField(key, indexKey));
  }

  protected BaseMapping addIndexBooleanField(String key, String indexKey) {
    return addField(key, new IndexBooleanField(key, indexKey));
  }

  protected BaseMapping addIndexDatetimeField(String key, String indexKey) {
    return addField(key, new IndexDatetimeField(key, indexKey));
  }

  protected BaseMapping addIndexArrayField(String key, String indexKey) {
    return addField(key, new IndexArrayField(key, indexKey));
  }

  protected BaseMapping addField(String key, Field field) {
    fields.put(key, field);
    if (field instanceof IndexField) {
      IndexField indexField = (IndexField) field;
      indexFields.putAll(key, Arrays.asList(indexField.indexFields()));
    }
    return this;
  }

  public static interface Field<D> {
    void write(JsonWriter json, D doc);
  }

  public static abstract class IndexField<D> implements Field<D> {
    protected final String[] indexFields;

    protected IndexField(String... indexFields) {
      this.indexFields = indexFields;
    }

    String[] indexFields() {
      return indexFields;
    }
  }

  /**
   * String field
   */
  public static class IndexStringField extends IndexField<BaseDoc> {
    private final String key;

    public IndexStringField(String key, String indexKey) {
      super(indexKey);
      this.key = key;
    }

    @Override
    public void write(JsonWriter json, BaseDoc doc) {
      Object val = doc.getField(indexFields[0]);
      json.prop(key, val != null ? val.toString() : null);
    }
  }

  public static class IndexBooleanField extends IndexField<BaseDoc> {
    private final String key;

    public IndexBooleanField(String key, String indexKey) {
      super(indexKey);
      this.key = key;
    }

    @Override
    public void write(JsonWriter json, BaseDoc doc) {
      Boolean val = doc.getField(indexFields[0]);
      json.prop(key, val != null ? val.booleanValue() : null);
    }
  }

  public static class IndexArrayField extends IndexField<BaseDoc> {
    private final String key;

    public IndexArrayField(String key, String indexKey) {
      super(indexKey);
      this.key = key;
    }

    @Override
    public void write(JsonWriter json, BaseDoc doc) {
      Iterable<String> values = doc.getField(indexFields[0]);
      json.name(key).beginArray().values(values).endArray();
    }
  }

  public static class IndexDatetimeField extends IndexField<BaseDoc> {
    private final String key;

    public IndexDatetimeField(String key, String indexKey) {
      super(indexKey);
      this.key = key;
    }

    @Override
    public void write(JsonWriter json, BaseDoc doc) {
      String val = doc.getField(indexFields[0]);
      if (val != null) {
        json.propDateTime(key, IndexUtils.parseDateTime(val));
      }
    }
  }

}
