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
package org.sonar.duplications.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Groups a set of related {@link ClonePart}s.
 */
public class CloneGroup {

  private final ClonePart originPart;
  private final int cloneLength;
  private final List<ClonePart> parts;
  private int length;

  /**
   * Cache for hash code.
   */
  private int hash;

  /**
   * @since 2.14
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * @since 2.14
   */
  public static final class Builder {
    private ClonePart origin;
    private int length;
    private int lengthInUnits;
    private List<ClonePart> parts = new ArrayList<>();

    public Builder setLength(int length) {
      this.length = length;
      return this;
    }

    public Builder setOrigin(ClonePart origin) {
      this.origin = origin;
      return this;
    }

    public Builder setParts(List<ClonePart> parts) {
      this.parts = new ArrayList<>(parts);
      return this;
    }

    public Builder addPart(ClonePart part) {
      Objects.requireNonNull(part);
      this.parts.add(part);
      return this;
    }

    public Builder setLengthInUnits(int length) {
      this.lengthInUnits = length;
      return this;
    }

    public CloneGroup build() {
      return new CloneGroup(this);
    }
  }

  private CloneGroup(Builder builder) {
    this.cloneLength = builder.length;
    this.originPart = builder.origin;
    this.parts = builder.parts;
    this.length = builder.lengthInUnits;
  }

  public ClonePart getOriginPart() {
    return originPart;
  }

  /**
   * Length of duplication measured in original units, e.g. for token-based detection - in tokens.
   *
   * @since 2.14
   */
  public int getLengthInUnits() {
    return length;
  }

  /**
   * @return clone length in {@link org.sonar.duplications.block.Block}s
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
