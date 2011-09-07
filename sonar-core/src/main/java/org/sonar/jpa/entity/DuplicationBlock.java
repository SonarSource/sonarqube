/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.jpa.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @since 2.11
 */
@Entity
@Table(name = "duplications_index")
public class DuplicationBlock {

  public static final int BLOCK_HASH_SIZE = 50;

  @Id
  @Column(name = "id")
  @GeneratedValue
  private Integer id;

  @Column(name = "snapshot_id", updatable = false, nullable = false)
  private Integer snapshotId;

  @Column(name = "project_snapshot_id", updatable = false, nullable = false)
  private Integer projectSnapshotId;

  @Column(name = "hash", updatable = false, nullable = false, length = BLOCK_HASH_SIZE)
  private String hash;

  @Column(name = "index_in_file", updatable = false, nullable = false)
  private Integer indexInFile;

  @Column(name = "start_line", updatable = false, nullable = false)
  private Integer startLine;

  @Column(name = "end_line", updatable = false, nullable = false)
  private Integer endLine;

  public DuplicationBlock() {
  }

  public DuplicationBlock(Integer projectSnapshotId, Integer snapshotId, String hash, Integer indexInFile, Integer startLine, Integer endLine) {
    this.projectSnapshotId = projectSnapshotId;
    this.snapshotId = snapshotId;
    this.hash = hash;
    this.indexInFile = indexInFile;
    this.startLine = startLine;
    this.endLine = endLine;
  }

  public Integer getId() {
    return id;
  }

  public Integer getSnapshotId() {
    return snapshotId;
  }

  public Integer getProjectSnapshotId() {
    return projectSnapshotId;
  }

  public String getHash() {
    return hash;
  }

  public Integer getIndexInFile() {
    return indexInFile;
  }

  public Integer getStartLine() {
    return startLine;
  }

  public Integer getEndLine() {
    return endLine;
  }

}
