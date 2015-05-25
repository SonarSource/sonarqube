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
import java.util.Collections;
import java.util.List;

/**
 * @deprecated since 4.5.2 replace by highlighting mechanism
 */
@Deprecated
public final class JavaTokenizers {

  private static final List<Tokenizer> TOKENIZERS = Collections.unmodifiableList(Arrays.asList(
    new JavaAnnotationTokenizer("<span class=\"a\">", "</span>"),
    new LiteralTokenizer("<span class=\"s\">", "</span>"),
    new CDocTokenizer("<span class=\"cd\">", "</span>"),
    new JavadocTokenizer("<span class=\"j\">", "</span>"),
    new CppDocTokenizer("<span class=\"cppd\">", "</span>"),
    new JavaConstantTokenizer("<span class=\"c\">", "</span>"),
    new KeywordsTokenizer("<span class=\"k\">", "</span>", JavaKeywords.get())));

  private JavaTokenizers() {
    // only static stuff
  }

  public static List<Tokenizer> forHtml() {
    return TOKENIZERS;
  }
}
