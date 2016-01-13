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

import javax.annotation.Nullable;

import java.io.Reader;
import java.util.List;

/**
 * @deprecated since 4.5.2 replace by highlighting mechanism
 */
@Deprecated
public class CodeColorizer {

  private List<Tokenizer> tokenizers = null;

  public CodeColorizer(List<Tokenizer> tokenizers) {
    this.tokenizers = tokenizers;
  }

  public CodeColorizer(Format format) {
    this.tokenizers = format.getTokenizers();
  }

  public String toHtml(Reader code, @Nullable HtmlOptions options) {
    HtmlOptions opts = options == null ? HtmlOptions.DEFAULT : options;
    return new HtmlRenderer(opts).render(code, tokenizers);
  }

  public static String javaToHtml(Reader code, HtmlOptions options) {
    return new CodeColorizer(Format.JAVA).toHtml(code, options);
  }

  public static String groovyToHtml(Reader code, HtmlOptions options) {
    return new CodeColorizer(Format.GROOVY).toHtml(code, options);
  }

  public enum Format {
    JAVA(JavaTokenizers.forHtml()), GROOVY(GroovyTokenizers.forHtml());

    private List<Tokenizer> tokenizers;

    Format(List<Tokenizer> tokenizers) {
      this.tokenizers = tokenizers;
    }

    public List<Tokenizer> getTokenizers() {
      return tokenizers;
    }
  }
}
