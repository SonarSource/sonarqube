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

package org.sonar.batch.source;

import com.google.common.base.Objects;
import org.sonar.api.batch.fs.TextRange;

import java.io.Serializable;

public class DefaultSymbol implements org.sonar.api.source.Symbol, Serializable {

  private TextRange range;
  private int length;

  public DefaultSymbol(TextRange range, int length) {
    this.range = range;
    this.length = length;
  }

  @Override
  public int getDeclarationStartOffset() {
    throw new UnsupportedOperationException("getDeclarationStartOffset");
  }

  @Override
  public int getDeclarationEndOffset() {
    throw new UnsupportedOperationException("getDeclarationEndOffset");
  }

  @Override
  public String getFullyQualifiedName() {
    throw new UnsupportedOperationException("getFullyQualifiedName");
  }

  public TextRange range() {
    return range;
  }

  public int getLength() {
    return length;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper("Symbol")
      .add("range", range)
      .toString();
  }
}
