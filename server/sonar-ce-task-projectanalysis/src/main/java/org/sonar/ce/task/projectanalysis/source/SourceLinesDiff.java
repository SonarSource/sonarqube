/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.source;

import org.sonar.ce.task.projectanalysis.component.Component;

public interface SourceLinesDiff {
  /**
   * Creates a diff between the file in the database and the file in the report using Myers' algorithm, and links matching lines between
   * both files.
   *
   * <p>When {@code useReferenceBranchForNcd} is {@code true} (REFERENCE_BRANCH NCD with no SCM in the report) the DB side of the
   * diff is loaded from the reference branch's file rather than via {@link OriginalFileResolver}. Used by
   * {@code ScmInfoRepositoryImpl} so that the returned {@code matchingLines} stays consistent with the
   * {@code DbScmInfo} that {@code ScmInfoDbLoader} produced on the same call.
   *
   * @return an array with one entry for each line in the left side. Those entries point either to a line in the right side, or to 0,
   * in which case it means the line was added.
   */
  int[] computeMatchingLines(Component component, boolean useReferenceBranchForNcd);
}
