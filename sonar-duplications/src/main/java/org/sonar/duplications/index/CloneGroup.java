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
package org.sonar.duplications.index;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * Groups a set of related {@link ClonePart}s.
 */
public class CloneGroup {

  private final ClonePart originPart;
  private final int cloneLength;
  private final List<ClonePart> parts;

  /**
   * Cache for hash code.
   */
  private int hash;

  public CloneGroup(int cloneLength, ClonePart origin, List<ClonePart> parts) {
    this.cloneLength = cloneLength;
    this.originPart = origin;
    this.parts = ImmutableList.copyOf(parts);
  }

  public ClonePart getOriginPart() {
    return originPart;
  }

  /**
   * @return clone length in units (not in lines)
   */
  public int getCloneUnitLength() {
    return cloneLength;
  }

  public List<ClonePart> getCloneParts() {
    return parts;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (ClonePart part : parts) {
      builder.append(part).append(" - ");
    }
    builder.append(cloneLength);
    return builder.toString();
  }

  /**
   * Two groups are equal, if they have same length, same origins and contain same parts in same order.
   */
  @Override
  public boolean equals(Object object) {
    if (!(object instanceof CloneGroup)) {
      return false;
    }
    CloneGroup another = (CloneGroup) object;
    if (another.cloneLength != cloneLength || parts.size() != another.parts.size()) {
      return false;
    }
    if (!originPart.equals(another.originPart)) {
      return false;
    }
    boolean result = true;
    for (int i = 0; i < parts.size(); i++) {
      result &= another.parts.get(i).equals(parts.get(i));
    }
    return result;
  }

  @Override
  public int hashCode() {
    int h = hash;
    if (h == 0 && cloneLength != 0) {
      for (ClonePart part : parts) {
        h = 31 * h + part.hashCode();
      }
      h = 31 * h + originPart.hashCode();
      h = 31 * h + cloneLength;
      hash = h;
    }
    return h;
  }

}
