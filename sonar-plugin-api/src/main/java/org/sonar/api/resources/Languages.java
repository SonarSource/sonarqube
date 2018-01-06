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
package org.sonar.api.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.ArrayUtils;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * A class to store the list of languages
 *
 * @since 1.10
 */
@ScannerSide
@ServerSide
@ComputeEngineSide
public class Languages {

  private static final Logger LOG = Loggers.get(Languages.class);

  private final Map<String, Language> map = new LinkedHashMap<>();

  /**
   * Creates a list of languages
   */
  public Languages(Language... languages) {
    LOG.debug("Available languages:");
    for (Language language : languages) {
      map.put(language.getKey(), language);
      LOG.debug("  * " + language.getName() + " => \"" + language.getKey() + "\"");
    }
  }

  /**
   * No languages are installed
   */
  public Languages() {
    LOG.debug("No language available");
  }

  /**
   * @param keys the languages keys
   * @return the list of suffix files associates to languages included in the current object
   */
  public String[] getSuffixes(String... keys) {
    List<String> suffixes = new ArrayList<>();

    for (Map.Entry<String, Language> entry : map.entrySet()) {
      if (ArrayUtils.isEmpty(keys) || ArrayUtils.contains(keys, entry.getKey())) {
        suffixes.addAll(Arrays.asList(entry.getValue().getFileSuffixes()));
      }
    }
    return suffixes.toArray(new String[suffixes.size()]);
  }

  /**
   * Return a language from the current object based on its key
   */
  public Language get(String key) {
    return map.get(key);
  }

  /**
   * Adds a language to the current object
   */
  public void add(Language language) {
    map.put(language.getKey(), language);
  }

  /**
   * @since 4.2
   */
  public Language[] all() {
    Collection<Language> languages = map.values();
    return languages.toArray(new Language[languages.size()]);
  }

}
