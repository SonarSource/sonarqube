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
package org.sonar.duplications.internal.pmd;

import com.google.common.base.Preconditions;
import org.sonar.duplications.CodeFragment;

/**
 * Immutable code fragment, which formed from tokens of one line.
 */
class TokensLine implements CodeFragment {

  private final int startLine;
  private final int hashCode;

  private final int startUnit;
  private final int endUnit;

  public TokensLine(int startUnit, int endUnit, int startLine, int hashCode) {
    Preconditions.checkArgument(startLine > 0);
    // TODO do we have requirements for length and hashcode ?
    this.startLine = startLine;
    this.hashCode = hashCode;

    this.startUnit = startUnit;
    this.endUnit = endUnit;
  }

  public int getStartLine() {
    return startLine;
  }

  /**
   * Same as {@link #getStartLine()}
   */
  public int getEndLine() {
    return startLine;
  }

  public int getHashCode() {
    return hashCode;
  }

  public int getStartUnit() {
    return startUnit;
  }

  public int getEndUnit() {
    return endUnit;
  }

}
