/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.es;

import com.google.common.collect.ImmutableMap;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.server.es.IndexType.IndexMainType;
import org.sonar.server.es.IndexType.IndexRelationType;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.sonar.server.es.IndexType.FIELD_INDEX_TYPE;

/**
 * Base implementation for business objects based on elasticsearch document
 */
public abstract class BaseDoc {

  private static final String SETPARENT_NOT_CALLED = "parent must be set on a doc associated to a IndexRelationType (see BaseDoc#setParent(String))";
  private final IndexType indexType;
  private String parentId = null;
  protected final Map<String, Object> fields;

  protected BaseDoc(IndexType indexType) {
    this(indexType, new HashMap<>());
  }

  protected BaseDoc(IndexType indexType, Map<String, Object> fields) {
    this.indexType = indexType;
    this.fields = fields;
    if (indexType instanceof IndexMainType) {
      IndexMainType mainType = (IndexMainType) indexType;
      if (mainType.getIndex().acceptsRelations()) {
        setField(mainType.getIndex().getJoinField(), ImmutableMap.of("name", mainType.getType()));
        setField(FIELD_INDEX_TYPE, mainType.getType());
      }
    }
  }

  protected void setParent(String parentId) {
    checkState(this.indexType instanceof IndexRelationType, "Doc must be associated to a IndexRelationType to set a parent");
    checkArgument(parentId != null && !parentId.isEmpty(), "parentId can't be null nor empty");
    this.parentId = parentId;
    IndexRelationType indexRelationType = (IndexRelationType) this.indexType;
    setField(indexRelationType.getMainType().getIndex().getJoinField(), ImmutableMap.of("name", indexRelationType.getName(), "parent", parentId));
    setField(FIELD_INDEX_TYPE, indexRelationType.getName());
  }

  public abstract String getId();

  public Optional<String> getRouting() {
    // when using relations, routing MUST be defined and MUST be the id of the parent
    if (this.indexType instanceof IndexRelationType) {
      ensureSetParentCalled();
      return Optional.of(this.parentId);
    }
    if (this.indexType instanceof IndexMainType && indexType.getMainType().getIndex().acceptsRelations()) {
      return Optional.of(getId());
    }
    return getSimpleMainTypeRouting();
  }

  /**
   * Intended to be overridden by subclass which wants to define a routing and which indexType is a {@link IndexMainType}
   * (if not a {@link IndexMainType}, this method will never be called).
   */
  protected Optional<String> getSimpleMainTypeRouting() {
    return Optional.empty();
  }

  /**
   * Use this method when field value can be null. See warning in {@link #getField(String)}
   */
  @CheckForNull
  public <K> K getNullableField(String key) {
    if (!fields.containsKey(key)) {
      throw new IllegalStateException(String.format("Field %s not specified in query options", key));
    }
    return (K) fields.get(key);
  }

  @CheckForNull
  public Date getNullableFieldAsDate(String key) {
    Object val = getNullableField(key);
    if (val != null) {
      if (val instanceof Date) {
        return (Date) val;
      }
      if (val instanceof Number) {
        return epochSecondsToDate((Number) val);
      }
      return EsUtils.parseDateTime((String) val);
    }
    return null;
  }

  /**
   * Use this method when you are sure that the value can't be null in ES document.
   * <p/>
   * Warning with numbers - even if mapping declares long field, value can be an Integer
   * instead of an expected Long. The reason is that ES delegates the deserialization of JSON
   * to Jackson, which doesn't know the field type declared in mapping. See
   * https://groups.google.com/forum/#!searchin/elasticsearch/getsource$20integer$20long/elasticsearch/jxIY22TmA8U/PyqZPPyYQ0gJ
   * for more details. Workaround is to cast to java.lang.Number and then to call {@link Number#longValue()}
   */
  public <K> K getField(String key) {
    K value = getNullableField(key);
    if (value == null) {
      throw new IllegalStateException("Value of index field is null: " + key);
    }
    return value;
  }

  public Date getFieldAsDate(String key) {
    Object value = getField(key);
    if (value instanceof Date) {
      return (Date) value;
    }
    if (value instanceof Number) {
      return epochSecondsToDate((Number) value);
    }
    return EsUtils.parseDateTime((String) value);
  }

  public void setField(String key, @Nullable Object value) {
    fields.put(key, value);
  }

  public final Map<String, Object> getFields() {
    if (indexType instanceof IndexRelationType) {
      ensureSetParentCalled();
    }
    return fields;
  }

  private void ensureSetParentCalled() {
    checkState(this.parentId != null, SETPARENT_NOT_CALLED);
  }

  public IndexRequest toIndexRequest() {
    IndexMainType mainType = this.indexType.getMainType();
    return new IndexRequest(mainType.getIndex().getName(), mainType.getType())
      .id(getId())
      .routing(getRouting().orElse(null))
      .source(getFields());
  }

  public static long epochMillisToEpochSeconds(long epochMillis) {
    return epochMillis / 1000L;
  }

  private static Date epochSecondsToDate(Number value) {
    return new Date(value.longValue() * 1000L);
  }

  public static long dateToEpochSeconds(Date date) {
    return epochMillisToEpochSeconds(date.getTime());
  }
}
