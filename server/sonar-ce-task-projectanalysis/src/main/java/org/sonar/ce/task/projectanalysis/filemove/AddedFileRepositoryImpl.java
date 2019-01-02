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
package org.sonar.ce.task.projectanalysis.filemove;

import java.util.HashSet;
import java.util.Set;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class AddedFileRepositoryImpl implements MutableAddedFileRepository {
  private final Set<Component> addedComponents = new HashSet<>();
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public AddedFileRepositoryImpl(AnalysisMetadataHolder analysisMetadataHolder) {
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public boolean isAdded(Component component) {
    checkComponent(component);
    if (analysisMetadataHolder.isFirstAnalysis()) {
      return true;
    }
    return addedComponents.contains(component);
  }

  @Override
  public void register(Component component) {
    checkComponent(component);
    checkArgument(component.getType() == Component.Type.FILE, "component must be a file");
    checkState(!analysisMetadataHolder.isFirstAnalysis(), "No file can be registered on first analysis");

    addedComponents.add(component);
  }

  private static void checkComponent(Component component) {
    checkNotNull(component, "component can't be null");
  }
}
