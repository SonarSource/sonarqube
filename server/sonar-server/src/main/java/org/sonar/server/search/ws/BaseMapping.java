/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
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
import java.util.Arrays;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.es.BaseDoc;
import org.sonar.server.search.IndexUtils;
import org.sonar.server.search.QueryContext;

/**
 * Mapping of search documents (see {@link BaseDoc}) to WS JSON responses
 * @deprecated no more "framework" to convert ES docs to WS responses.
 */
@ServerSide
@Deprecated
public abstract class BaseMapping<DOC extends BaseDoc, CTX> {

  private final Multimap<String, String> indexFieldsByWsFields = LinkedHashMultimap.create();
  private final Multimap<String, Mapper> mappers = LinkedHashMultimap.create();

  protected BaseMapping() {
    // Nothing here
  }

  /**
   * All the WS supported fields
   */
  public Set<String> supportedFields() {
    return mappers.keySet();
  }

  /**
   * Write only requested document fields
   */
  protected void doWrite(DOC doc, @Nullable CTX context, JsonWriter json, @Nullable QueryContext queryContext) {
    json.beginObject();
    json.prop("key", doc.keyField());
    if (queryContext == null || queryContext.getFieldsToReturn().isEmpty()) {
      // return all fields
      for (Mapper mapper : mappers.values()) {
        mapper.write(json, doc, context);
      }
    } else {
      for (String optionField : queryContext.getFieldsToReturn()) {
        for (Mapper mapper : mappers.get(optionField)) {
          mapper.write(json, doc, context);
        }
      }
    }
    json.endObject();
  }

  protected BaseMapping map(String key, String indexKey) {
    return map(key, new IndexStringMapper(key, indexKey));
  }

  protected BaseMapping mapDateTime(String key, String indexKey) {
    return map(key, new IndexDatetimeMapper(key, indexKey));
  }

  protected BaseMapping map(String key, Mapper mapper) {
    mappers.put(key, mapper);
    if (mapper instanceof IndexMapper) {
      IndexMapper indexField = (IndexMapper) mapper;
      indexFieldsByWsFields.putAll(key, Arrays.asList(indexField.indexFields()));
    }
    return this;
  }

  @FunctionalInterface
  public interface Mapper<DOC extends BaseDoc, CTX> {
    void write(JsonWriter json, DOC doc, CTX context);
  }

  public abstract static class IndexMapper<DOC extends BaseDoc, CTX> implements Mapper<DOC, CTX> {
    protected final String[] indexFields;

    protected IndexMapper(String... indexFields) {
      this.indexFields = indexFields;
    }

    String[] indexFields() {
      return indexFields;
    }
  }

  /**
   * String field
   */
  public static class IndexStringMapper<DOC extends BaseDoc, CTX> extends IndexMapper<DOC, CTX> {
    protected final String key;

    public IndexStringMapper(String key, String indexKey) {
      super(indexKey);
      this.key = key;
    }

    @Override
    public void write(JsonWriter json, DOC doc, CTX context) {
      Object val = doc.getNullableField(indexFields[0]);
      if (val == null && indexFields.length == 2) {
        // There is an alternative value
        val = doc.getNullableField(indexFields[1]);
      }
      json.prop(key, val != null ? val.toString() : null);
    }
  }

  public static class IndexDatetimeMapper<DOC extends BaseDoc, CTX> extends IndexMapper<DOC, CTX> {
    private final String key;

    public IndexDatetimeMapper(String key, String indexKey) {
      super(indexKey);
      this.key = key;
    }

    @Override
    public void write(JsonWriter json, DOC doc, CTX context) {
      String val = doc.getNullableField(indexFields[0]);
      if (val != null) {
        json.propDateTime(key, IndexUtils.parseDateTime(val));
      }
    }
  }

}
