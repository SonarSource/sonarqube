/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.colorizer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @deprecated since 4.5.2 replace by highlighting mechanism
 */
@Deprecated
public final class GroovyKeywords {

  private static final Set<String> KEYWORDS = new HashSet<>();

  static {
    KEYWORDS.addAll(Arrays.asList("as", "assert", "break", "case", "catch", "class", "continue", "def",
      "default", "do", "else", "extends", "finally", "for", "if", "in", "implements", "import", "instanceof", "interface", "new", "package",
      "property", "return", "switch", "throw", "throws", "try", "while"));
  }

  private GroovyKeywords() {
  }

  public static Set<String> get() {
    return KEYWORDS;
  }
}
