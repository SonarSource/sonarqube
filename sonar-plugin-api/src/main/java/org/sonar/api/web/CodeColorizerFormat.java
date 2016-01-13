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
package org.sonar.api.web;

import com.google.common.base.Objects;
import org.sonar.api.task.TaskExtension;
import org.sonar.colorizer.Tokenizer;

import java.util.List;

/**
 * Extend the library sonar-colorizer to support new languages.
 * 
 * @since 1.12
 * @deprecated since 4.5.2 use {@link org.sonar.api.source.Highlightable}
 */
@Deprecated
public abstract class CodeColorizerFormat implements TaskExtension {

  private String languageKey;

  /**
   * @param languageKey the unique sonar key. Not null.
   */
  protected CodeColorizerFormat(String languageKey) {
    this.languageKey = languageKey;
  }

  public final String getLanguageKey() {
    return languageKey;
  }

  /**
   * sonar-colorizer tokenizers for HTML output.
   * 
   * @return a not null list (empty if no tokenizers)
   */
  public abstract List<Tokenizer> getTokenizers();

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CodeColorizerFormat)) {
      return false;
    }

    CodeColorizerFormat format = (CodeColorizerFormat) o;
    return languageKey.equals(format.languageKey);

  }

  @Override
  public int hashCode() {
    return languageKey.hashCode();
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("lang", languageKey)
      .toString();
  }
}
