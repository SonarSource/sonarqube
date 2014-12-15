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

import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Resource;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public class BatchResource {

  private final long batchId;
  private final Resource r;
  private final Snapshot s;
  private final BatchResource parent;
  private final Collection<BatchResource> children = new ArrayList<BatchResource>();

  public BatchResource(long batchId, Resource r, Snapshot s, @Nullable BatchResource parent) {
    this.batchId = batchId;
    this.r = r;
    this.s = s;
    this.parent = parent;
    if (parent != null) {
      parent.children.add(this);
    }
  }

  public long batchId() {
    return batchId;
  }

  public Resource resource() {
    return r;
  }

  public int snapshotId() {
    return s.getId();
  }

  public Snapshot snapshot() {
    return s;
  }

  @CheckForNull
  public BatchResource parent() {
    return parent;
  }

  public Collection<BatchResource> children() {
    return children;
  }
}
