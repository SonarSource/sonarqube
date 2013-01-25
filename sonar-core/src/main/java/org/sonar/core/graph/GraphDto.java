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
package org.sonar.core.graph;

public class GraphDto {
  private long snapshotId;
  private String format;
  private String perspective;
  private int version;
  private String rootVertexId;
  private String data;

  public long getSnapshotId() {
    return snapshotId;
  }

  public GraphDto setSnapshotId(long snapshotId) {
    this.snapshotId = snapshotId;
    return this;
  }

  public String getPerspective() {
    return perspective;
  }

  public GraphDto setPerspective(String perspective) {
    this.perspective = perspective;
    return this;
  }

  public String getFormat() {
    return format;
  }

  public GraphDto setFormat(String format) {
    this.format = format;
    return this;
  }

  public int getVersion() {
    return version;
  }

  public GraphDto setVersion(int version) {
    this.version = version;
    return this;
  }

  public String getRootVertexId() {
    return rootVertexId;
  }

  public GraphDto setRootVertexId(String rootVertexId) {
    this.rootVertexId = rootVertexId;
    return this;
  }

  public String getData() {
    return data;
  }

  public GraphDto setData(String data) {
    this.data = data;
    return this;
  }
}
