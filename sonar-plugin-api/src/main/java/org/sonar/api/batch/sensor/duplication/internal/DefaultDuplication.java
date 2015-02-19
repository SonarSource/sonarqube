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
package org.sonar.api.batch.sensor.duplication.internal;

import org.sonar.api.batch.sensor.internal.SensorStorage;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.duplication.Duplication;
import org.sonar.api.batch.sensor.duplication.NewDuplication;
import org.sonar.api.batch.sensor.internal.DefaultStorable;

import javax.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;

public class DefaultDuplication extends DefaultStorable implements NewDuplication, Duplication {

  private Block originBlock;
  private List<Block> duplicates = new LinkedList<Duplication.Block>();

  public DefaultDuplication() {
    super();
  }

  public DefaultDuplication(@Nullable SensorStorage storage) {
    super(storage);
  }

  @Override
  public DefaultDuplication originBlock(InputFile inputFile, int startLine, int endLine) {
    Preconditions.checkArgument(inputFile != null, "InputFile can't be null");
    validateLineArgument(inputFile, startLine, "startLine");
    validateLineArgument(inputFile, endLine, "endLine");
    originBlock = new Duplication.Block(((DefaultInputFile) inputFile).key(), startLine, endLine - startLine + 1);
    return this;
  }

  @Override
  public DefaultDuplication isDuplicatedBy(InputFile sameOrOtherFile, int startLine, int endLine) {
    Preconditions.checkArgument(sameOrOtherFile != null, "InputFile can't be null");
    validateLineArgument(sameOrOtherFile, startLine, "startLine");
    validateLineArgument(sameOrOtherFile, endLine, "endLine");
    return isDuplicatedBy(((DefaultInputFile) sameOrOtherFile).key(), startLine, endLine);
  }

  /**
   * For internal use. Global duplications are referencing files outside of current project so
   * no way to manipulate an InputFile.
   */
  public DefaultDuplication isDuplicatedBy(String fileKey, int startLine, int endLine) {
    Preconditions.checkNotNull(originBlock, "Call originBlock() first");
    duplicates.add(new Duplication.Block(fileKey, startLine, endLine - startLine + 1));
    return this;
  }

  @Override
  public void doSave() {
    Preconditions.checkNotNull(originBlock, "Call originBlock() first");
    Preconditions.checkState(!duplicates.isEmpty(), "No duplicates. Call isDuplicatedBy()");
    storage.store(this);
  }

  @Override
  public Block originBlock() {
    return originBlock;
  }

  public DefaultDuplication setOriginBlock(Block originBlock) {
    this.originBlock = originBlock;
    return this;
  }

  @Override
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
    DefaultDuplication rhs = (DefaultDuplication) obj;
    EqualsBuilder equalsBuilder = new EqualsBuilder()
      .append(originBlock, rhs.originBlock)
      .append(duplicates.size(), rhs.duplicates.size());
    if (duplicates.size() == rhs.duplicates.size()) {
      for (int i = 0; i < duplicates.size(); i++) {
        equalsBuilder.append(duplicates.get(i), rhs.duplicates.get(i));
      }
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
