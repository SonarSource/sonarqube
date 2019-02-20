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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.es.IndexType.FIELD_INDEX_TYPE;
import static org.sonar.server.es.newindex.DefaultIndexSettings.ANALYZER;
import static org.sonar.server.es.newindex.DefaultIndexSettings.FIELD_TYPE_TEXT;
import static org.sonar.server.es.newindex.DefaultIndexSettings.INDEX;
import static org.sonar.server.es.newindex.DefaultIndexSettings.TYPE;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.UUID_MODULE_ANALYZER;

public abstract class FieldAware<U extends FieldAware<U>> {

  abstract U setFieldImpl(String fieldName, Object attributes);

  protected final U setField(String fieldName, Object attributes) {
    checkArgument(!FIELD_INDEX_TYPE.equalsIgnoreCase(fieldName), "%s is a reserved field name", FIELD_INDEX_TYPE);
    return setFieldImpl(fieldName, attributes);
  }

  @SuppressWarnings("unchecked")
  public KeywordFieldBuilder<U> keywordFieldBuilder(String fieldName) {
    return (KeywordFieldBuilder<U>) new KeywordFieldBuilder(this, fieldName);
  }

  @SuppressWarnings("unchecked")
  public TextFieldBuilder<U> textFieldBuilder(String fieldName) {
    return (TextFieldBuilder<U>) new TextFieldBuilder(this, fieldName);
  }

  @SuppressWarnings("unchecked")
  public NestedFieldBuilder<U> nestedFieldBuilder(String fieldName) {
    return (NestedFieldBuilder<U>) new NestedFieldBuilder(this, fieldName);
  }

  public U createBooleanField(String fieldName) {
    return setField(fieldName, ImmutableMap.of("type", "boolean"));
  }

  public U createByteField(String fieldName) {
    return setField(fieldName, ImmutableMap.of("type", "byte"));
  }

  public U createDateTimeField(String fieldName) {
    Map<String, String> hash = new TreeMap<>();
    hash.put("type", "date");
    hash.put("format", "date_time||epoch_second");
    return setField(fieldName, hash);
  }

  public U createDoubleField(String fieldName) {
    return setField(fieldName, ImmutableMap.of("type", "double"));
  }

  public U createIntegerField(String fieldName) {
    return setField(fieldName, ImmutableMap.of("type", "integer"));
  }

  public U createLongField(String fieldName) {
    return setField(fieldName, ImmutableMap.of("type", "long"));
  }

  public U createShortField(String fieldName) {
    return setField(fieldName, ImmutableMap.of("type", "short"));
  }

  public U createUuidPathField(String fieldName) {
    return setField(fieldName, ImmutableSortedMap.of(
      TYPE, FIELD_TYPE_TEXT,
      INDEX, DefaultIndexSettings.INDEX_SEARCHABLE,
      ANALYZER, UUID_MODULE_ANALYZER.getName()));
  }

}
