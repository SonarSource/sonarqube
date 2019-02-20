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
package org.sonar.server.es.textsearch;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.es.newindex.DefaultIndexSettings;

import static org.sonar.server.es.newindex.DefaultIndexSettings.MINIMUM_NGRAM_LENGTH;

/**
 * Splits text queries into their tokens, for to use them in n_gram match queries later.
 */
public class JavaTokenizer {

  private JavaTokenizer() {
    // use static methods
  }

  public static List<String> split(String queryText) {
    return Arrays.stream(
      queryText.split(DefaultIndexSettings.SEARCH_TERM_TOKENIZER_PATTERN))
      .filter(StringUtils::isNotEmpty)
      .filter(s -> s.length() >= MINIMUM_NGRAM_LENGTH)
      .collect(MoreCollectors.toList());
  }
}
