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
package org.sonar.server.es;

import com.google.common.collect.ImmutableSortedMap;
import java.util.Arrays;
import java.util.Locale;
import java.util.SortedMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;

import static org.sonar.server.es.DefaultIndexSettings.ANALYSIS;
import static org.sonar.server.es.DefaultIndexSettings.ANALYZER;
import static org.sonar.server.es.DefaultIndexSettings.DELIMITER;
import static org.sonar.server.es.DefaultIndexSettings.FILTER;
import static org.sonar.server.es.DefaultIndexSettings.KEYWORD;
import static org.sonar.server.es.DefaultIndexSettings.LOWERCASE;
import static org.sonar.server.es.DefaultIndexSettings.PATTERN;
import static org.sonar.server.es.DefaultIndexSettings.SUB_FIELD_DELIMITER;
import static org.sonar.server.es.DefaultIndexSettings.TOKENIZER;
import static org.sonar.server.es.DefaultIndexSettings.TYPE;

public enum DefaultIndexSettingsElement {

  // Filters

  EDGE_NGRAM_FILTER(FILTER) {

    @Override
    protected void setup() {
      set(TYPE, "edge_ngram");
      set("min_gram", 1);
      set("max_gram", 15);
    }
  },

  // Tokenizers

  CAMEL_CASE_TOKENIZER(TOKENIZER) {

    @Override
    protected void setup() {
      set(TYPE, "pattern");
      set(PATTERN, "(?=[\\p{Lu}\\d])");
    }
  },

  // Analyzers

  CAMEL_CASE_ANALYZER(ANALYZER) {

    @Override
    protected void setup() {
      set(TOKENIZER, CAMEL_CASE_TOKENIZER);
      setArray(FILTER, EDGE_NGRAM_FILTER);
    }

    @Override
    public SortedMap<String, String> fieldMapping() {
      return ImmutableSortedMap.of(
        "type", "string",
        "index", "analyzed",
        "analyzer", getName());
    }
  },
  FUZZY_ANALYZER(ANALYZER) {

    @Override
    protected void setup() {
      set(TOKENIZER, KEYWORD);
      setArray(FILTER, LOWERCASE);
    }

    @Override
    public SortedMap<String, String> fieldMapping() {
      return ImmutableSortedMap.of(
        "type", "string",
        "index", "analyzed",
        "analyzer", getName());
    }
  },

  ;

  private final String type;
  private final String name;

  private Builder builder = Settings.builder();

  DefaultIndexSettingsElement(String type) {
    this.type = type;
    this.name = name().toLowerCase(Locale.getDefault());
    setup();
  }

  protected void set(String settingSuffix, int value) {
    put(localName(settingSuffix), Integer.toString(value));
  }

  protected void set(String settingSuffix, DefaultIndexSettingsElement otherElement) {
    put(localName(settingSuffix), otherElement.name);
  }

  protected void set(String settingSuffix, String value) {
    put(localName(settingSuffix), value);
  }

  protected void setArray(String settingSuffix, String... values) {
    putArray(localName(settingSuffix), values);
  }

  protected void setArray(String settingSuffix, DefaultIndexSettingsElement... values) {
    putArray(localName(settingSuffix), Arrays.stream(values).map(DefaultIndexSettingsElement::getName).toArray(String[]::new));
  }

  private void put(String setting, String value) {
    builder = builder.put(setting, value);
  }

  private void putArray(String setting, String... values) {
    builder = builder.putArray(setting, values);
  }

  private String localName(String settingSuffix) {
    return ANALYSIS + DELIMITER + type + DELIMITER + name + DELIMITER + settingSuffix;
  }

  public Settings settings() {
    return builder.build();
  }

  protected abstract void setup();

  public SortedMap<String, String> fieldMapping() {
    throw new UnsupportedOperationException("The elasticsearch configuration element '" + name + "' cannot be used as field mapping.");
  }

  public String subField(String fieldName) {
    return fieldName + SUB_FIELD_DELIMITER + getSubFieldSuffix();
  }

  public String getSubFieldSuffix() {
    return getName();
  }

  public String getName() {
    return name;
  }
}
