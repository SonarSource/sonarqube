/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.sensors;

import org.sonar.api.batch.Phase;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.jpa.dao.AsyncMeasuresService;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ResourceUtils;

@Phase(name = Phase.Name.PRE)
public class AsynchronousMeasuresSensor implements Sensor {

  private AsyncMeasuresService reviewsService;
  private Snapshot snapshot;

  public AsynchronousMeasuresSensor(AsyncMeasuresService reviewsService, Snapshot snapshot) {
    this.reviewsService = reviewsService;
    this.snapshot = snapshot;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void analyse(Project project, SensorContext context) {
    if (!ResourceUtils.isRootProject(project)) {
      return;
    }
    reviewsService.refresh(snapshot);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
