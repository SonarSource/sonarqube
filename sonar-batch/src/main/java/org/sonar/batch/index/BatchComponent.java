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
package org.sonar.batch.index;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public class BatchComponent {

  private final int batchId;
  private final Resource r;
  private Snapshot s;
  private final BatchComponent parent;
  private final Collection<BatchComponent> children = new ArrayList<>();
  private InputPath inputPath;

  public BatchComponent(int batchId, Resource r, @Nullable BatchComponent parent) {
    this.batchId = batchId;
    this.r = r;
    this.parent = parent;
    if (parent != null) {
      parent.children.add(this);
    }
  }

  public String key() {
    return r.getEffectiveKey();
  }

  public int batchId() {
    return batchId;
  }

  public Resource resource() {
    return r;
  }

  public BatchComponent setSnapshot(Snapshot snapshot) {
    this.s = snapshot;
    return this;
  }

  /**
   * @return null in database less mode
   */
  @CheckForNull
  public Integer snapshotId() {
    return s != null ? s.getId() : null;
  }

  public Snapshot snapshot() {
    return s;
  }

  @CheckForNull
  public BatchComponent parent() {
    return parent;
  }

  public Collection<BatchComponent> children() {
    return children;
  }

  public boolean isFile() {
    return Qualifiers.isFile(r) || StringUtils.equals(Qualifiers.UNIT_TEST_FILE, r.getQualifier());
  }

  public boolean isDir() {
    return Qualifiers.isDirectory(r);
  }

  public BatchComponent setInputPath(InputPath inputPath) {
    this.inputPath = inputPath;
    return this;
  }

  @CheckForNull
  public InputPath inputPath() {
    return inputPath;
  }
}
