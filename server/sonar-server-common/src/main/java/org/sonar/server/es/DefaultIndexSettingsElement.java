/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.collect.ImmutableSortedMap;
import java.util.Arrays;
import java.util.Locale;
import java.util.SortedMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;

import static org.sonar.server.es.DefaultIndexSettings.ANALYSIS;
import static org.sonar.server.es.DefaultIndexSettings.ANALYZER;
import static org.sonar.server.es.DefaultIndexSettings.ASCIIFOLDING;
import static org.sonar.server.es.DefaultIndexSettings.CHAR_FILTER;
import static org.sonar.server.es.DefaultIndexSettings.DELIMITER;
import static org.sonar.server.es.DefaultIndexSettings.FIELDDATA_ENABLED;
import static org.sonar.server.es.DefaultIndexSettings.FIELD_FIELDDATA;
import static org.sonar.server.es.DefaultIndexSettings.FIELD_TYPE_TEXT;
import static org.sonar.server.es.DefaultIndexSettings.FILTER;
import static org.sonar.server.es.DefaultIndexSettings.HTML_STRIP;
import static org.sonar.server.es.DefaultIndexSettings.INDEX;
import static org.sonar.server.es.DefaultIndexSettings.INDEX_SEARCHABLE;
import static org.sonar.server.es.DefaultIndexSettings.KEYWORD;
import static org.sonar.server.es.DefaultIndexSettings.LOWERCASE;
import static org.sonar.server.es.DefaultIndexSettings.MAXIMUM_NGRAM_LENGTH;
import static org.sonar.server.es.DefaultIndexSettings.MAX_GRAM;
import static org.sonar.server.es.DefaultIndexSettings.MINIMUM_NGRAM_LENGTH;
import static org.sonar.server.es.DefaultIndexSettings.MIN_GRAM;
import static org.sonar.server.es.DefaultIndexSettings.PATTERN;
import static org.sonar.server.es.DefaultIndexSettings.PORTER_STEM;
import static org.sonar.server.es.DefaultIndexSettings.SEARCH_ANALYZER;
import static org.sonar.server.es.DefaultIndexSettings.STANDARD;
import static org.sonar.server.es.DefaultIndexSettings.STOP;
import static org.sonar.server.es.DefaultIndexSettings.SUB_FIELD_DELIMITER;
import static org.sonar.server.es.DefaultIndexSettings.TOKENIZER;
import static org.sonar.server.es.DefaultIndexSettings.TRIM;
import static org.sonar.server.es.DefaultIndexSettings.TYPE;
import static org.sonar.server.es.DefaultIndexSettings.WHITESPACE;

public enum DefaultIndexSettingsElement {

  // Filters

  WORD_FILTER(FILTER) {

    @Override
    protected void setup() {
      set(TYPE, "word_delimiter");
      set("generate_word_parts", true);
      set("catenate_words", true);
      set("catenate_numbers", true);
      set("catenate_all", true);
      set("split_on_case_change", true);
      set("preserve_original", true);
      set("split_on_numerics", true);
      set("stem_english_possessive", true);
    }
  },
  NGRAM_FILTER(FILTER) {

    @Override
    protected void setup() {
      set(TYPE, "nGram");
      set(MIN_GRAM, MINIMUM_NGRAM_LENGTH);
      set(MAX_GRAM, MAXIMUM_NGRAM_LENGTH);
      setArray("token_chars", "letter", "digit", "punctuation", "symbol");
    }
  },

  // Tokenizers

  GRAM_TOKENIZER(TOKENIZER) {

    @Override
    protected void setup() {
      set(TYPE, "nGram");
      set(MIN_GRAM, MINIMUM_NGRAM_LENGTH);
      set(MAX_GRAM, MAXIMUM_NGRAM_LENGTH);
      setArray("token_chars", "letter", "digit", "punctuation", "symbol");
    }
  },
  PREFIX_TOKENIZER(TOKENIZER) {

    @Override
    protected void setup() {
      set(TYPE, "edgeNGram");
      set(MIN_GRAM, MINIMUM_NGRAM_LENGTH);
      set(MAX_GRAM, MAXIMUM_NGRAM_LENGTH);
    }
  },
  UUID_MODULE_TOKENIZER(TOKENIZER) {

    @Override
    protected void setup() {
      set(TYPE, PATTERN);
      set(PATTERN, "\\.");
    }
  },

  // Analyzers

  SORTABLE_ANALYZER(ANALYZER) {

    @Override
    protected void setup() {
      set(TOKENIZER, KEYWORD);
      setArray(FILTER, TRIM, LOWERCASE);
    }

    @Override
    public SortedMap<String, String> fieldMapping() {
      return ImmutableSortedMap.of(
        TYPE, FIELD_TYPE_TEXT,
        INDEX, INDEX_SEARCHABLE,
        ANALYZER, getName(),
        FIELD_FIELDDATA, FIELDDATA_ENABLED);
    }
  },
  INDEX_GRAMS_ANALYZER(ANALYZER) {

    @Override
    protected void setup() {
      set(TOKENIZER, GRAM_TOKENIZER);
      setArray(FILTER, TRIM, LOWERCASE);
    }
  },
  SEARCH_GRAMS_ANALYZER(ANALYZER) {

    @Override
    protected void setup() {
      set(TOKENIZER, WHITESPACE);
      setArray(FILTER, TRIM, LOWERCASE);
    }

    @Override
    public SortedMap<String, String> fieldMapping() {
      return ImmutableSortedMap.of(
        TYPE, FIELD_TYPE_TEXT,
        INDEX, INDEX_SEARCHABLE,
        ANALYZER, INDEX_GRAMS_ANALYZER.getName(),
        SEARCH_ANALYZER, getName());
    }
  },
  INDEX_PREFIX_ANALYZER(ANALYZER) {

    @Override
    protected void setup() {
      set(TOKENIZER, PREFIX_TOKENIZER);
      setArray(FILTER, TRIM);
    }
  },
  SEARCH_PREFIX_ANALYZER(ANALYZER) {

    @Override
    protected void setup() {
      set(TOKENIZER, WHITESPACE);
      setArray(FILTER, TRIM);
    }

    @Override
    public SortedMap<String, String> fieldMapping() {
      return ImmutableSortedMap.of(
        TYPE, FIELD_TYPE_TEXT,
        INDEX, INDEX_SEARCHABLE,
        ANALYZER, INDEX_PREFIX_ANALYZER.getName(),
        SEARCH_ANALYZER, getName());
    }
  },
  INDEX_PREFIX_CASE_INSENSITIVE_ANALYZER(ANALYZER) {

    @Override
    protected void setup() {
      set(TOKENIZER, PREFIX_TOKENIZER);
      setArray(FILTER, TRIM, LOWERCASE);
    }
  },
  SEARCH_PREFIX_CASE_INSENSITIVE_ANALYZER(ANALYZER) {

    @Override
    protected void setup() {
      set(TOKENIZER, WHITESPACE);
      setArray(FILTER, TRIM, LOWERCASE);
    }

    @Override
    public SortedMap<String, String> fieldMapping() {
      return ImmutableSortedMap.of(
        TYPE, FIELD_TYPE_TEXT,
        INDEX, INDEX_SEARCHABLE,
        ANALYZER, INDEX_PREFIX_CASE_INSENSITIVE_ANALYZER.getName(),
        SEARCH_ANALYZER, getName());
    }
  },
  USER_INDEX_GRAMS_ANALYZER(ANALYZER) {

    @Override
    protected void setup() {
      set(TOKENIZER, WHITESPACE);
      setArray(FILTER, TRIM, LOWERCASE, NGRAM_FILTER.getName());
    }
  },
  USER_SEARCH_GRAMS_ANALYZER(ANALYZER) {

    @Override
    protected void setup() {
      set(TOKENIZER, WHITESPACE);
      setArray(FILTER, TRIM, LOWERCASE);
    }

    @Override
    public SortedMap<String, String> fieldMapping() {
      return ImmutableSortedMap.of(
        TYPE, FIELD_TYPE_TEXT,
        INDEX, INDEX_SEARCHABLE,
        ANALYZER, USER_INDEX_GRAMS_ANALYZER.getName(),
        SEARCH_ANALYZER, getName());
    }
  },
  INDEX_WORDS_ANALYZER(ANALYZER) {

    @Override
    protected void setup() {
      set(TOKENIZER, STANDARD);
      setArray(FILTER, STANDARD, "word_filter", LOWERCASE, STOP, ASCIIFOLDING, PORTER_STEM);
    }
  },
  SEARCH_WORDS_ANALYZER(ANALYZER) {

    @Override
    protected void setup() {
      set(TOKENIZER, STANDARD);
      setArray(FILTER, STANDARD, LOWERCASE, STOP, ASCIIFOLDING, PORTER_STEM);
    }

    @Override
    public SortedMap<String, String> fieldMapping() {
      return ImmutableSortedMap.of(
        TYPE, FIELD_TYPE_TEXT,
        INDEX, INDEX_SEARCHABLE,
        ANALYZER, INDEX_WORDS_ANALYZER.getName(),
        SEARCH_ANALYZER, getName());
    }
  },
  ENGLISH_HTML_ANALYZER(ANALYZER) {

    @Override
    protected void setup() {
      set(TOKENIZER, STANDARD);
      setArray(FILTER, STANDARD, LOWERCASE, STOP, ASCIIFOLDING, PORTER_STEM);
      setArray(CHAR_FILTER, HTML_STRIP);
    }

    @Override
    public SortedMap<String, String> fieldMapping() {
      return ImmutableSortedMap.of(
        TYPE, FIELD_TYPE_TEXT,
        INDEX, INDEX_SEARCHABLE,
        ANALYZER, getName());
    }
  },
  PATH_ANALYZER(ANALYZER) {

    @Override
    protected void setup() {
      set(TOKENIZER, "path_hierarchy");
    }
  },
  UUID_MODULE_ANALYZER(ANALYZER) {

    @Override
    protected void setup() {
      set(TOKENIZER, UUID_MODULE_TOKENIZER);
      setArray(FILTER, TRIM);
    }
  },

  ;

  private final String type;
  private final String name;

  private Builder builder = Settings.builder();

  DefaultIndexSettingsElement(String type) {
    this.type = type;
    this.name = name().toLowerCase(Locale.ENGLISH);
    setup();
  }

  protected void set(String settingSuffix, int value) {
    put(localName(settingSuffix), Integer.toString(value));
  }

  protected void set(String settingSuffix, boolean value) {
    put(localName(settingSuffix), Boolean.toString(value));
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
