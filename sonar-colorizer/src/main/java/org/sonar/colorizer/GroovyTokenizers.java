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
package org.sonar.colorizer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @deprecated since 4.5.2 replace by highlighting mechanism
 */
@Deprecated
public final class GroovyTokenizers {

  private static final String SPAN_END = "</span>";

  private GroovyTokenizers() {
  }

  public static List<Tokenizer> forHtml() {
    return Collections.unmodifiableList(Arrays.asList(new JavaAnnotationTokenizer("<span class=\"a\">", SPAN_END), new LiteralTokenizer(
      "<span class=\"s\">", SPAN_END), new CDocTokenizer("<span class=\"cd\">", SPAN_END), new CppDocTokenizer("<span class=\"cppd\">",
      SPAN_END), new JavadocTokenizer("<span class=\"j\">", SPAN_END), new JavaConstantTokenizer("<span class=\"c\">", SPAN_END),
      new KeywordsTokenizer("<span class=\"k\">", SPAN_END, GroovyKeywords.get())));
  }
}
