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
package org.sonar.api.batch.sensor.duplication;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.batch.sensor.SensorContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link DuplicationGroup} is a list of duplicated {@link Block}s. One block
 * is considered as the original code and all others are duplicates.
 * Use {@link SensorContext#duplicationBuilder(org.sonar.api.batch.fs.InputFile)} and
 * {@link SensorContext#saveDuplications(org.sonar.api.batch.fs.InputFile, List)}.
 * @since 4.5
 */
public class DuplicationGroup {

  public static class Block {
    private final String resourceKey;
    private final int startLine;
    private final int length;

    public Block(String resourceKey, int startLine, int length) {
      this.resourceKey = resourceKey;
      this.startLine = startLine;
      this.length = length;
    }

    public String resourceKey() {
      return resourceKey;
    }

    public int startLine() {
      return startLine;
    }

    public int length() {
      return length;
    }

    // Just for unit tests
    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (obj == this) {
        return true;
      }
      if (obj.getClass() != getClass()) {
        return false;
      }
      Block rhs = (Block) obj;
      return new EqualsBuilder()
        .append(resourceKey, rhs.resourceKey)
        .append(startLine, rhs.startLine)
        .append(length, rhs.length).isEquals();
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).
        append("resourceKey", resourceKey).
        append("startLine", startLine).
        append("length", length).
        toString();
    }
  }

  private final Block originBlock;
  private List<Block> duplicates = new ArrayList<DuplicationGroup.Block>();

  /**
   * For unit test and internal use only.
   */
  public DuplicationGroup(Block originBlock) {
    this.originBlock = originBlock;
  }

  /**
   * For unit test and internal use only.
   */
  public void setDuplicates(List<Block> duplicates) {
    this.duplicates = duplicates;
  }

  /**
   * For unit test and internal use only.
   */
  public DuplicationGroup addDuplicate(Block anotherBlock) {
    this.duplicates.add(anotherBlock);
    return this;
  }

  public Block originBlock() {
    return originBlock;
  }

  public List<Block> duplicates() {
    return duplicates;
  }

  // Just for unit tests
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }
    DuplicationGroup rhs = (DuplicationGroup) obj;
    EqualsBuilder equalsBuilder = new EqualsBuilder()
      .append(originBlock, rhs.originBlock)
      .append(duplicates.size(), rhs.duplicates.size());
    for (int i = 0; i < duplicates.size(); i++) {
      equalsBuilder.append(duplicates.get(i), rhs.duplicates.get(i));
    }
    return equalsBuilder.isEquals();
  }

  @Override
  public int hashCode() {
    HashCodeBuilder hcBuilder = new HashCodeBuilder(17, 37)
      .append(originBlock)
      .append(duplicates.size());
    for (int i = 0; i < duplicates.size(); i++) {
      hcBuilder.append(duplicates.get(i));
    }
    return hcBuilder.toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).
      append("origin", originBlock).
      append("duplicates", duplicates, true).
      toString();
  }

}
