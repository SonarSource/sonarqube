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
package org.sonar.ce.task.projectanalysis.dbmigration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.of;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

/**
 * Implementation of {@link ProjectAnalysisDataChanges} based on an ordered list of {@link ProjectAnalysisDataChange}
 * classes and the {@link ProjectAnalysisDataChange} instances which can be injected by the container.
 */
public class ProjectAnalysisDataChangesImpl implements ProjectAnalysisDataChanges {
  private static final List<Class<? extends ProjectAnalysisDataChange>> DATA_CHANGE_CLASSES_IN_ORDER_OF_EXECUTION = of(
    PopulateFileSourceLineCount.class);
  private final List<ProjectAnalysisDataChange> dataChangeInstances;

  public ProjectAnalysisDataChangesImpl(ProjectAnalysisDataChange[] dataChanges) {
    checkArgument(dataChanges.length == DATA_CHANGE_CLASSES_IN_ORDER_OF_EXECUTION.size(),
      "Number of ProjectAnalysisDataChange instance available (%s) is inconsistent with the number of declared ProjectAnalysisDataChange types (%s)",
      dataChanges.length,
      DATA_CHANGE_CLASSES_IN_ORDER_OF_EXECUTION.size());
    Map<? extends Class<? extends ProjectAnalysisDataChange>, ProjectAnalysisDataChange> dataChangesByClass = Arrays.stream(dataChanges)
      .collect(uniqueIndex(ProjectAnalysisDataChange::getClass));
    dataChangeInstances = DATA_CHANGE_CLASSES_IN_ORDER_OF_EXECUTION.stream()
      .map(dataChangesByClass::get)
      .filter(Objects::nonNull)
      .collect(toList(DATA_CHANGE_CLASSES_IN_ORDER_OF_EXECUTION.size()));
    checkState(dataChangeInstances.size() == DATA_CHANGE_CLASSES_IN_ORDER_OF_EXECUTION.size(),
      "Some of the ProjectAnalysisDataChange type declared have no instance in the container");
  }

  static List<Class<? extends ProjectAnalysisDataChange>> getDataChangeClasses() {
    return DATA_CHANGE_CLASSES_IN_ORDER_OF_EXECUTION;
  }

  @Override
  public List<ProjectAnalysisDataChange> getDataChanges() {
    return dataChangeInstances;
  }
}
