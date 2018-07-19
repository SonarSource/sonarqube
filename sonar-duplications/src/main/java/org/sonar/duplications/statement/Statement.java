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
package org.sonar.duplications.statement;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.duplications.CodeFragment;
import org.sonar.duplications.token.Token;

public class Statement implements CodeFragment {

  private final int startLine;
  private final int endLine;
  private final String value;

  /**
   * Cache for hash code.
   */
  private int hash;

  public Statement(int startLine, int endLine, String value) {
    this.startLine = startLine;
    this.endLine = endLine;
    this.value = value;
  }

  public Statement(@Nullable List<Token> tokens) {
    if (tokens == null || tokens.isEmpty()) {
      throw new IllegalArgumentException("A statement can't be initialized with an empty list of tokens");
    }
    StringBuilder sb = new StringBuilder();
    for (Token token : tokens) {
      sb.append(token.getValue());
    }
    this.value = sb.toString();
    this.startLine = tokens.get(0).getLine();
    this.endLine = tokens.get(tokens.size() - 1).getLine();
  }

  @Override
  public int getStartLine() {
    return startLine;
  }

  @Override
  public int getEndLine() {
    return endLine;
  }

  public String getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    int h = hash;
    if (h == 0) {
      h = value.hashCode();
      h = 31 * h + startLine;
      h = 31 * h + endLine;
      hash = h;
    }
    return h;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Statement)) {
      return false;
    }
    Statement other = (Statement) obj;
    return startLine == other.startLine
      && endLine == other.endLine
      && value.equals(other.value);
  }

  @Override
  public String toString() {
    return "[" + getStartLine() + "-" + getEndLine() + "] [" + getValue() + "]";
  }

}
