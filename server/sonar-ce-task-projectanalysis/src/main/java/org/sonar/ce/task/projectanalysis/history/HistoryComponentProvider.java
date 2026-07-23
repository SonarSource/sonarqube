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
package org.sonar.ce.task.projectanalysis.history;

import java.util.ArrayList;
import java.util.List;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.ce.task.projectanalysis.container.ReportAnalysisComponentProvider;
import org.sonarsource.history.server.HistoryServerComponents;

/** Provides history recording components to the CE analysis container. */
@ComputeEngineSide
public class HistoryComponentProvider implements ReportAnalysisComponentProvider {
  /** Returns the delegate and shared recording components used during analysis. */
  @Override
  public List<Object> getComponents() {
    List<Object> components = new ArrayList<>();
    components.add(RecordHistoryDelegateImpl.class);
    components.addAll(HistoryServerComponents.recordingComponents());
    return components;
  }
}
