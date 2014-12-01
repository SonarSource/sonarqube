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

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * @deprecated since 4.5.2 replace by highlighting mechanism
 */
@Deprecated
public final class JavaKeywords {

  private static final Set<String> KEYWORDS = ImmutableSet.of(
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
    "class", "const", "continue", "default",
    "do", "double", "else", "enum", "extends", "false", "final", "finally", "float", "for",
    "goto", "if", "implements", "import", "instanceof",
    "int", "interface", "long", "native", "new", "null", "package", "private",
    "protected", "public", "return", "short", "static", "strictfp",
    "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "void", "volatile", "while");

  private JavaKeywords() {
  }

  public static Set<String> get() {
    return KEYWORDS;
  }
}
