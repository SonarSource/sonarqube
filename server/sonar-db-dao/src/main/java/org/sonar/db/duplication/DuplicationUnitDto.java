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
package org.sonar.db.duplication;

public final class DuplicationUnitDto {

  private long id;
  private String analysisUuid;
  private String componentUuid;

  private String hash;
  private int indexInFile;
  private int startLine;
  private int endLine;

  // Return by join
  private String componentKey;

  public long getId() {
    return id;
  }

  public DuplicationUnitDto setId(long id) {
    this.id = id;
    return this;
  }

  public String getAnalysisUuid() {
    return analysisUuid;
  }

  public DuplicationUnitDto setAnalysisUuid(String analysisUuid) {
    this.analysisUuid = analysisUuid;
    return this;
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public DuplicationUnitDto setComponentUuid(String componentUuid) {
    this.componentUuid = componentUuid;
    return this;
  }

  public String getHash() {
    return hash;
  }

  public DuplicationUnitDto setHash(String hash) {
    this.hash = hash;
    return this;
  }

  public int getIndexInFile() {
    return indexInFile;
  }

  public DuplicationUnitDto setIndexInFile(int indexInFile) {
    this.indexInFile = indexInFile;
    return this;
  }

  public int getStartLine() {
    return startLine;
  }

  public DuplicationUnitDto setStartLine(int startLine) {
    this.startLine = startLine;
    return this;
  }

  public int getEndLine() {
    return endLine;
  }

  public DuplicationUnitDto setEndLine(int endLine) {
    this.endLine = endLine;
    return this;
  }

  public String getComponentKey() {
    return componentKey;
  }

}
