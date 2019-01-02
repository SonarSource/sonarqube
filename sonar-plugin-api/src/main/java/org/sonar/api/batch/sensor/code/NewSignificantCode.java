/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.batch.sensor.code;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;

/**
 * This object is used to report ranges of a file which contain significant code.
 * Lines that are left out (no range reported) will be ignored and won't be considered when calculating which lines were modified.
 * It's particularly useful when portions of the lines are automatically modified by code editors and do not contain  
 * code or comments were issues might be created. 
 * 
 * Don't forget to call {@link #save()} after setting the file and the ranges where the significant code is.
 * 
 * @since 7.2
 */
public interface NewSignificantCode {
  /**
   * The file for which significant code is being reported. This field must be set before saving the error.
   */
  NewSignificantCode onFile(InputFile file);

  /**
   * Add a range of significant code. The range may only include a single line and only one range is accepted per line.
   */
  NewSignificantCode addRange(TextRange range);

  /**
   * Save the reported information for the given file.
   */
  void save();
}
