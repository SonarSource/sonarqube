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
package org.sonar.server.es.newindex;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.lang.String.valueOf;
import static org.sonar.server.es.newindex.DefaultIndexSettings.FIELDDATA_ENABLED;
import static org.sonar.server.es.newindex.DefaultIndexSettings.FIELD_FIELDDATA;
import static org.sonar.server.es.newindex.DefaultIndexSettings.FIELD_TERM_VECTOR;
import static org.sonar.server.es.newindex.DefaultIndexSettings.FIELD_TYPE_KEYWORD;
import static org.sonar.server.es.newindex.DefaultIndexSettings.INDEX;
import static org.sonar.server.es.newindex.DefaultIndexSettings.INDEX_NOT_SEARCHABLE;
import static org.sonar.server.es.newindex.DefaultIndexSettings.INDEX_SEARCHABLE;
import static org.sonar.server.es.newindex.DefaultIndexSettings.NORMS;
import static org.sonar.server.es.newindex.DefaultIndexSettings.STORE;

/**
 * Helper to define a string field in mapping of index type
 */
public abstract class StringFieldBuilder<U extends FieldAware<U>, T extends StringFieldBuilder<U, T>> {
  private final U parent;
  private final String fieldName;
  private boolean disableSearch = false;
  private boolean disableNorms = false;
  private boolean termVectorWithPositionOffsets = false;
  private SortedMap<String, Object> subFields = Maps.newTreeMap();
  private boolean store = false;
  protected boolean disabledDocValues = false;

  protected StringFieldBuilder(U parent, String fieldName) {
    this.parent = parent;
    this.fieldName = fieldName;
  }

  /**
   * Add a sub-field. A {@code SortedMap} is required for consistency of the index settings hash.
   */
  private T addSubField(String fieldName, SortedMap<String, String> fieldDefinition) {
    subFields.put(fieldName, fieldDefinition);
    return castThis();
  }

  /**
   * Add subfields, one for each analyzer.
   */
  public T addSubFields(DefaultIndexSettingsElement... analyzers) {
    Arrays.stream(analyzers)
      .forEach(analyzer -> addSubField(analyzer.getSubFieldSuffix(), analyzer.fieldMapping()));
    return castThis();
  }

  /**
   * Norms consume useless memory if string field is used for filtering or aggregations.
   *
   * https://www.elastic.co/guide/en/elasticsearch/reference/2.3/norms.html
   * https://www.elastic.co/guide/en/elasticsearch/guide/current/scoring-theory.html#field-norm
   */
  public T disableNorms() {
    this.disableNorms = true;
    return castThis();
  }

  /**
   * Position offset term vectors are required for the fast_vector_highlighter (fvh).
   */
  public T termVectorWithPositionOffsets() {
    this.termVectorWithPositionOffsets = true;
    return castThis();
  }

  /**
   * "index: false" -> Make this field not searchable.
   * By default field is "true": it is searchable, but index the value exactly
   * as specified.
   */
  public T disableSearch() {
    this.disableSearch = true;
    return castThis();
  }

  public T store() {
    this.store = true;
    return castThis();
  }

  @SuppressWarnings("unchecked")
  private T castThis() {
    return (T) this;
  }

  public U build() {
    if (subFields.isEmpty()) {
      return buildWithoutSubfields();
    }
    return buildWithSubfields();
  }

  private U buildWithoutSubfields() {
    Map<String, Object> hash = new TreeMap<>();
    hash.put("type", getFieldType());
    hash.put(INDEX, disableSearch ? INDEX_NOT_SEARCHABLE : INDEX_SEARCHABLE);
    hash.put(NORMS, valueOf(!disableNorms));
    hash.put(STORE, valueOf(store));
    if (FIELD_TYPE_KEYWORD.equals(getFieldType())) {
      hash.put("doc_values", valueOf(!disabledDocValues));
    }
    if (getFieldData()) {
      hash.put(FIELD_FIELDDATA, FIELDDATA_ENABLED);
    }
    return parent.setField(fieldName, hash);
  }

  private U buildWithSubfields() {
    Map<String, Object> hash = new TreeMap<>();
    hash.put("type", getFieldType());
    hash.put(INDEX, disableSearch ? INDEX_NOT_SEARCHABLE : INDEX_SEARCHABLE);
    hash.put(NORMS, "false");
    hash.put(STORE, valueOf(store));
    if (FIELD_TYPE_KEYWORD.equals(getFieldType())) {
      hash.put("doc_values", valueOf(!disabledDocValues));
    }
    if (getFieldData()) {
      hash.put(FIELD_FIELDDATA, FIELDDATA_ENABLED);
    }
    if (termVectorWithPositionOffsets) {
      hash.put(FIELD_TERM_VECTOR, "with_positions_offsets");
    }
    hash.put("fields", configureSubFields());
    return parent.setField(fieldName, hash);
  }

  private Map<String, Object> configureSubFields() {
    Map<String, Object> multiFields = new TreeMap<>(subFields);

    // apply this fields configuration to all subfields
    multiFields.entrySet().forEach(entry -> {
      Object subFieldMapping = entry.getValue();
      if (subFieldMapping instanceof Map) {
        entry.setValue(configureSubField((Map<String, String>) subFieldMapping));
      }
    });
    return multiFields;
  }

  private Map<String, String> configureSubField(Map<String, String> subFieldMapping) {
    Map<String, String> subHash = new TreeMap<>(subFieldMapping);
    subHash.put(INDEX, INDEX_SEARCHABLE);
    subHash.put(NORMS, "false");
    subHash.put(STORE, valueOf(store));
    if (termVectorWithPositionOffsets) {
      subHash.put(FIELD_TERM_VECTOR, "with_positions_offsets");
    }
    return subHash;
  }

  protected abstract boolean getFieldData();

  protected abstract String getFieldType();
}
