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
package org.sonar.server.computation.task.projectanalysis.source;

import org.sonar.core.util.CloseableIterator;
import org.sonar.server.computation.task.projectanalysis.component.Component;

public interface SourceLinesRepository {

  /**
   * Creates a iterator over the source lines of a given component from the report.
   * <p>
   * The returned {@link CloseableIterator} will wrap the {@link CloseableIterator} returned by
   * {@link org.sonar.server.computation.task.projectanalysis.batch.BatchReportReader#readFileSource(int)} but enforces that the number
   * of lines specified by {@link org.sonar.scanner.protocol.output.ScannerReport.Component#getLines()} is respected, adding
   * an extra empty last line if required.
   * </p>
   *
   * @throws NullPointerException if argument is {@code null}
   * @throws IllegalArgumentException if component is not a {@link Component.Type#FILE}
   * @throws IllegalStateException if the file has no source code in the report
   */
  CloseableIterator<String> readLines(Component component);
}
