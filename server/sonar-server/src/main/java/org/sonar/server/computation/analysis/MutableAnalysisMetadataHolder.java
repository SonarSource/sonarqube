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
package org.sonar.server.computation.analysis;

import javax.annotation.Nullable;
import org.sonar.server.computation.snapshot.Snapshot;

public interface MutableAnalysisMetadataHolder extends AnalysisMetadataHolder {

  /**
   * @throws IllegalStateException if the analysis date has already been set
   */
  MutableAnalysisMetadataHolder setAnalysisDate(long date);

  /**
   * @throws IllegalStateException if baseProjectSnapshot has already been set
   */
  MutableAnalysisMetadataHolder setBaseProjectSnapshot(@Nullable Snapshot baseProjectSnapshot);

  /**
   * @throws IllegalStateException if cross project duplication flag has already been set
   */
  MutableAnalysisMetadataHolder setCrossProjectDuplicationEnabled(boolean isCrossProjectDuplicationEnabled);

  /**
   * @throws IllegalStateException if branch has already been set
   */
  MutableAnalysisMetadataHolder setBranch(@Nullable String branch);

  /**
   * @throws IllegalStateException if root component ref has already been set
   */
  MutableAnalysisMetadataHolder setRootComponentRef(int rootComponentRef);

}
