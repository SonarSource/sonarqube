/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.colorizer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class JavaKeywords {

  private static final Set<String> KEYWORDS = new HashSet<String>();

  private static final String[] JAVA_KEYWORDS = new String[] { "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
      "class", "const", "continue", "default",
 "do", "double", "else", "enum", "extends", "false", "final", "finally", "float", "for",
      "goto", "if", "implements", "import", "instanceof",
 "int", "interface", "long", "native", "new", "null", "package", "private",
      "protected", "public", "return", "short", "static", "strictfp",
      "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "void", "volatile", "while"};

  static {
    Collections.addAll(KEYWORDS, JAVA_KEYWORDS);
  }

  private JavaKeywords() {
  }

  public static Set<String> get() {
    return Collections.unmodifiableSet(KEYWORDS);
  }
}
