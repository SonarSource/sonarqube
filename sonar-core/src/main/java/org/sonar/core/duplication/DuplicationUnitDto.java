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
package org.sonar.core.duplication;


/**
 * A simple DTO (Data Transfer Object) class that provides the mapping of data to a table.
 */
public final class DuplicationUnitDto {

  private Long id;
  private Integer snapshotId;
  private Integer projectSnapshotId;

  private String hash;
  private int indexInFile;
  private int startLine;
  private int endLine;

  private String resourceKey;

  public DuplicationUnitDto() {
  }

  public DuplicationUnitDto(Integer projectSnapshotId, Integer snapshotId, String hash, Integer indexInFile, Integer startLine, Integer endLine) {
    this.projectSnapshotId = projectSnapshotId;
    this.snapshotId = snapshotId;
    this.hash = hash;
    this.indexInFile = indexInFile;
    this.startLine = startLine;
    this.endLine = endLine;
  }

  public Long getId() {
    return id;
  }

  public DuplicationUnitDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Integer getSnapshotId() {
    return snapshotId;
  }

  public void setSnapshotId(Integer snapshotId) {
    this.snapshotId = snapshotId;
  }

  public Integer getProjectSnapshotId() {
    return projectSnapshotId;
  }

  public void setProjectSnapshotId(Integer projectSnapshotId) {
    this.projectSnapshotId = projectSnapshotId;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public int getIndexInFile() {
    return indexInFile;
  }

  public void setIndexInFile(int indexInFile) {
    this.indexInFile = indexInFile;
  }

  public int getStartLine() {
    return startLine;
  }

  public void setStartLine(int startLine) {
    this.startLine = startLine;
  }

  public int getEndLine() {
    return endLine;
  }

  public void setEndLine(int endLine) {
    this.endLine = endLine;
  }

  public String getResourceKey() {
    return resourceKey;
  }

  public void setResourceKey(String resourceKey) {
    this.resourceKey = resourceKey;
  }

}
