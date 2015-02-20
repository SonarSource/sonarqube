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

/**
 * <p/>
 * This builder is used to declare duplications on files of the project.
 * Usage:
 * <code><pre>
 * context.newDuplication();
 *   .originBlock(inputFile, 2, 10)
 *   .isDuplicatedBy(inputFile, 14, 22)
 *   .isDuplicatedBy(anotherInputFile, 3, 11)
 *   .save();
 * </pre></code>
 * @since 5.1
 */
@Beta
public interface NewDuplication {

  /**
   * Declare duplication origin block. Then call {@link #isDuplicatedBy(InputFile, int, int)} to reference all duplicates of this block.
   */
  NewDuplication originBlock(InputFile originFile, int startLine, int endLine);

  /**
   * Declare duplicate block of the previously declared {@link #originBlock(int, int)}. Can be called several times.
   * @param sameOrOtherFile duplicate can be in the same file or in another file.
   */
  NewDuplication isDuplicatedBy(InputFile sameOrOtherFile, int startLine, int endLine);

  /**
   * Save the duplication.
   */
  void save();
}
