/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.squid.api;

import java.util.HashSet;
import java.util.Set;

public class SourceFile extends SourceCode {

  private Set<Integer> noSonarTagLines = new HashSet<Integer>();

  public SourceFile(String key) {
    super(key);
    setStartAtLine(1);
  }

  public SourceFile(String key, String fileName) {
    super(key, fileName);
    setStartAtLine(1);
  }

  public Set<Integer> getNoSonarTagLines() {
    return noSonarTagLines;
  }

  public boolean hasNoSonarTagAtLine(int lineNumber) {
    return noSonarTagLines.contains(lineNumber);
  }

  public void addNoSonarTagLines(Set<Integer> noSonarTagLines) {
    this.noSonarTagLines.addAll(noSonarTagLines);
  }

  public void addNoSonarTagLine(int line) {
    noSonarTagLines.add(line);
  }

}
