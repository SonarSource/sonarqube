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

package org.sonar.server.computation.step;

import com.google.common.collect.Lists;
import org.sonar.api.ServerComponent;
import org.sonar.server.source.IndexSourceLinesStep;

import java.util.List;

public class ComputationStepRegistry implements ServerComponent {

  private final List<ComputationStep> steps;

  public ComputationStepRegistry(ComputationStep... s) {
    this.steps = order(s,
      DigestReportStep.class,
      ApplyPermissionsStep.class,
      SwitchSnapshotStep.class,
      InvalidatePreviewCacheStep.class,
      IndexComponentsStep.class,
      PurgeDatastoresStep.class,
      IndexIssuesStep.class,
      IndexSourceLinesStep.class);
  }

  public List<ComputationStep> steps() {
    return steps;
  }

  private List<ComputationStep> order(ComputationStep[] steps, Class<? extends ComputationStep>... classes) {
    List<ComputationStep> result = Lists.newArrayList();
    for (Class<? extends ComputationStep> clazz : classes) {
      result.add(find(steps, clazz));
    }
    return result;
  }

  private static ComputationStep find(ComputationStep[] steps, Class<? extends ComputationStep> clazz) {
    for (ComputationStep step : steps) {
      if (clazz.isInstance(step)) {
        return step;
      }
    }
    throw new IllegalStateException("Component not found in picocontainer: " + clazz);
  }
}
