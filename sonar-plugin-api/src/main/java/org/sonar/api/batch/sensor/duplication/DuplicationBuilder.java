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

import com.google.common.annotations.Beta;
import org.sonar.api.batch.fs.InputFile;

import java.util.List;

/**
 * Experimental, do not use.
 * <p/>
 * This builder is used to declare duplications on files of the project.
 * Usage:
 * <code><pre>
 * DuplicationBuilder builder = context.duplicationBuilder(inputFile);
 *   .originBlock(2, 10)
 *   .isDuplicatedBy(inputFile, 14, 22)
 *   .isDuplicatedBy(anotherInputFile, 3, 11)
 *   // Start another duplication
 *   .originBlock(45, 50)
 *   .isDuplicatedBy(yetAnotherInputFile, 10, 15);
 *   context.saveDuplications(inputFile, builder.build());
 * </pre></code>
 * @since 4.5
 */
@Beta
public interface DuplicationBuilder {

  /**
   * Declare duplication origin block. Then call {@link #isDuplicatedBy(InputFile, int, int)} to reference all duplicates of this block.
   * Then call again {@link #originBlock(int, int)} to declare another duplication.
   */
  DuplicationBuilder originBlock(int startLine, int endLine);

  /**
   * Declare duplicate block of the previously declared {@link #originBlock(int, int)}.
   * @param sameOrOtherFile duplicate can be in the same file or in another file.
   */
  DuplicationBuilder isDuplicatedBy(InputFile sameOrOtherFile, int startLine, int endLine);

  /**
   * Call this method when you have declared all duplications of the file.
   */
  List<DuplicationGroup> build();
}
