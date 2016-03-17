/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.index;

import com.google.common.collect.Lists;
import org.sonar.api.resources.Resource;

import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public final class Bucket {

  private Resource resource;

  private Bucket parent;
  private List<Bucket> children;

  public Bucket(Resource resource) {
    this.resource = resource;
  }

  public Resource getResource() {
    return resource;
  }

  public Bucket setParent(@Nullable Bucket parent) {
    this.parent = parent;
    if (parent != null) {
      parent.addChild(this);
    }
    return this;
  }

  private Bucket addChild(Bucket child) {
    if (children == null) {
      children = Lists.newArrayList();
    }
    children.add(child);
    return this;
  }

  private void removeChild(Bucket child) {
    if (children != null) {
      children.remove(child);
    }
  }

  public List<Bucket> getChildren() {
    return children == null ? Collections.<Bucket>emptyList() : children;
  }

  public Bucket getParent() {
    return parent;
  }

  public void clear() {
    children = null;
    if (parent != null) {
      parent.removeChild(this);
      parent = null;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Bucket that = (Bucket) o;
    return resource.equals(that.resource);
  }

  @Override
  public int hashCode() {
    return resource.hashCode();
  }
}
