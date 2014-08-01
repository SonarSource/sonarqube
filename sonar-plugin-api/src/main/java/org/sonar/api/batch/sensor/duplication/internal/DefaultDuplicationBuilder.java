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

import com.google.common.base.Preconditions;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.duplication.DuplicationBuilder;
import org.sonar.api.batch.sensor.duplication.DuplicationGroup;

import java.util.ArrayList;
import java.util.List;

public class DefaultDuplicationBuilder implements DuplicationBuilder {

  private final InputFile inputFile;
  private DuplicationGroup current = null;
  private List<DuplicationGroup> duplications;

  public DefaultDuplicationBuilder(InputFile inputFile) {
    this.inputFile = inputFile;
    duplications = new ArrayList<DuplicationGroup>();
  }

  @Override
  public DuplicationBuilder originBlock(int startLine, int endLine) {
    if (current != null) {
      duplications.add(current);
    }
    current = new DuplicationGroup(new DuplicationGroup.Block(((DefaultInputFile) inputFile).key(), startLine, endLine - startLine + 1));
    return this;
  }

  @Override
  public DuplicationBuilder isDuplicatedBy(InputFile sameOrOtherFile, int startLine, int endLine) {
    return isDuplicatedBy(((DefaultInputFile) sameOrOtherFile).key(), startLine, endLine);
  }

  /**
   * For internal use. Global duplications are referencing files outside of current project so
   * no way to manipulate an InputFile.
   */
  public DuplicationBuilder isDuplicatedBy(String fileKey, int startLine, int endLine) {
    Preconditions.checkNotNull(current, "Call originBlock() first");
    current.addDuplicate(new DuplicationGroup.Block(fileKey, startLine, endLine - startLine + 1));
    return this;
  }

  @Override
  public List<DuplicationGroup> build() {
    Preconditions.checkNotNull(current, "Call originBlock() first");
    duplications.add(current);
    List<DuplicationGroup> result = duplications;
    reset();
    return result;
  }

  private void reset() {
    duplications = new ArrayList<DuplicationGroup>();
    current = null;
  }
}
