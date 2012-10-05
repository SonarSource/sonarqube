/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.resources;

import com.google.common.collect.Maps;
import org.apache.commons.lang.ArrayUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A class to store the list of languages
 * 
 * @since 1.10
 */
public class Languages implements BatchComponent, ServerComponent {

  private final Map<String, Language> map = Maps.newHashMap();

  /**
   * Creates a list of languages
   */
  public Languages(Language... languages) {
    if (languages != null) {
      for (Language language : languages) {
        map.put(language.getKey(), language);
      }
    }
  }

  /**
   * @param keys the languages keys
   * @return the list of suffix files associates to languages included in the current object
   */
  public String[] getSuffixes(String... keys) {
    List<String> suffixes = new ArrayList<String>();

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
}
