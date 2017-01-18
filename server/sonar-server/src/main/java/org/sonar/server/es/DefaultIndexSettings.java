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

import java.util.Arrays;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;

public class DefaultIndexSettings {

  /** Maximum length of ngrams. */
  public static final int MAXIMUM_NGRAM_LENGTH = 15;

  public static final String ANALYSIS = "index.analysis";
  public static final String DELIMITER = ".";

  public static final String TOKENIZER = "tokenizer";
  public static final String FILTER = "filter";
  public static final String ANALYZER = "analyzer";

  public static final String TYPE = "type";
  public static final String PATTERN = "pattern";
  public static final String CUSTOM = "custom";
  public static final String KEYWORD = "keyword";

  public static final String SUB_FIELD_DELIMITER = ".";

  public static final String LOWERCASE = "lowercase";

  private DefaultIndexSettings() {
    // only static stuff
  }

  static Settings.Builder defaults() {
    Settings.Builder builder = Settings.builder()
      .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
      .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0)
      .put("index.refresh_interval", "30s")
      .put("index.mapper.dynamic", false)

      // Sortable text analyzer
      .put("index.analysis.analyzer.sortable.type", "custom")
      .put("index.analysis.analyzer.sortable.tokenizer", "keyword")
      .putArray("index.analysis.analyzer.sortable.filter", "trim", LOWERCASE)

      // NGram index-analyzer
      .put("index.analysis.analyzer.index_grams.type", "custom")
      .put("index.analysis.analyzer.index_grams.tokenizer", "gram_tokenizer")
      .putArray("index.analysis.analyzer.index_grams.filter", "trim", LOWERCASE)

      // NGram search-analyzer
      .put("index.analysis.analyzer.search_grams.type", "custom")
      .put("index.analysis.analyzer.search_grams.tokenizer", "whitespace")
      .putArray("index.analysis.analyzer.search_grams.filter", "trim", LOWERCASE)

      // Word index-analyzer
      .put("index.analysis.analyzer.index_words.type", "custom")
      .put("index.analysis.analyzer.index_words.tokenizer", "standard")
      .putArray("index.analysis.analyzer.index_words.filter",
        "standard", "word_filter", LOWERCASE, "stop", "asciifolding", "porter_stem")

      // Word search-analyzer
      .put("index.analysis.analyzer.search_words.type", "custom")
      .put("index.analysis.analyzer.search_words.tokenizer", "standard")
      .putArray("index.analysis.analyzer.search_words.filter",
        "standard", LOWERCASE, "stop", "asciifolding", "porter_stem")

      // English HTML analyzer
      .put("index.analysis.analyzer.html_analyzer.type", "custom")
      .put("index.analysis.analyzer.html_analyzer.tokenizer", "standard")
      .putArray("index.analysis.analyzer.html_analyzer.filter",
        "standard", LOWERCASE, "stop", "asciifolding", "porter_stem")
      .putArray("index.analysis.analyzer.html_analyzer.char_filter", "html_strip")

      // NGram tokenizer
      .put("index.analysis.tokenizer.gram_tokenizer.type", "nGram")
      .put("index.analysis.tokenizer.gram_tokenizer.min_gram", 2)
      .put("index.analysis.tokenizer.gram_tokenizer.max_gram", MAXIMUM_NGRAM_LENGTH)
      .putArray("index.analysis.tokenizer.gram_tokenizer.token_chars", "letter", "digit", "punctuation", "symbol")

      // Word filter
      .put("index.analysis.filter.word_filter.type", "word_delimiter")
      .put("index.analysis.filter.word_filter.generate_word_parts", true)
      .put("index.analysis.filter.word_filter.catenate_words", true)
      .put("index.analysis.filter.word_filter.catenate_numbers", true)
      .put("index.analysis.filter.word_filter.catenate_all", true)
      .put("index.analysis.filter.word_filter.split_on_case_change", true)
      .put("index.analysis.filter.word_filter.preserve_original", true)
      .put("index.analysis.filter.word_filter.split_on_numerics", true)
      .put("index.analysis.filter.word_filter.stem_english_possessive", true)

      // Path Analyzer
      .put("index.analysis.analyzer.path_analyzer.type", "custom")
      .put("index.analysis.analyzer.path_analyzer.tokenizer", "path_hierarchy")

      // UUID Module analyzer
      .put("index.analysis.tokenizer.dot_tokenizer.type", "pattern")
      .put("index.analysis.tokenizer.dot_tokenizer.pattern", "\\.")
      .put("index.analysis.analyzer.uuid_analyzer.type", "custom")
      .putArray("index.analysis.analyzer.uuid_analyzer.filter", "trim")
      .put("index.analysis.analyzer.uuid_analyzer.tokenizer", "dot_tokenizer");

    Arrays.stream(DefaultIndexSettingsElement.values())
      .map(DefaultIndexSettingsElement::settings)
      .forEach(builder::put);

    return builder;
  }
}
