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
package org.sonar.batch.protocol.output.resource;

import javax.annotation.CheckForNull;

import java.util.ArrayList;
import java.util.Collection;

public class ReportComponent {

  public enum Type {
    PRJ,
    MOD,
    DIR,
    FIL,
    VIEW,
    SUBVIEW
  }

  private long batchId;
  private int id;
  private int snapshotId;
  private String path;
  private String name;
  private Type type;
  // Only for files
  private Boolean isTest;
  private String languageKey;

  private Collection<ReportComponent> children = new ArrayList<ReportComponent>();

  public ReportComponent setBatchId(long batchId) {
    this.batchId = batchId;
    return this;
  }

  public long batchId() {
    return batchId;
  }

  public ReportComponent setId(int id) {
    this.id = id;
    return this;
  }

  public int id() {
    return id;
  }

  public ReportComponent setSnapshotId(int snapshotId) {
    this.snapshotId = snapshotId;
    return this;
  }

  public int snapshotId() {
    return snapshotId;
  }

  public ReportComponent setPath(String path) {
    this.path = path;
    return this;
  }

  public String path() {
    return path;
  }

  public ReportComponent setName(String name) {
    this.name = name;
    return this;
  }

  public String name() {
    return name;
  }

  public ReportComponent setType(Type type) {
    this.type = type;
    return this;
  }

  public Type type() {
    return type;
  }

  public ReportComponent setTest(Boolean isTest) {
    this.isTest = isTest;
    return this;
  }

  /**
   * @return null when not a file
   */
  @CheckForNull
  public Boolean isTest() {
    return isTest;
  }

  public ReportComponent setLanguageKey(String languageKey) {
    this.languageKey = languageKey;
    return this;
  }

  public String languageKey() {
    return languageKey;
  }

  public ReportComponent addChild(ReportComponent child) {
    this.children.add(child);
    return this;
  }

  public Collection<ReportComponent> children() {
    return children;
  }

}
