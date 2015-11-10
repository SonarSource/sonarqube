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
package org.sonar.batch.rule;

import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;

/**
 * Stores which Quality profiles have been used on the current module.
 *
 * TODO This information should not be stored as a measure but should be send as metadata in the {@link org.sonar.batch.protocol.output.BatchReport}
 */
public class QProfileSensor implements Sensor {

  private final ModuleQProfiles moduleQProfiles;
  private final FileSystem fs;
  private final AnalysisMode analysisMode;

  public QProfileSensor(ModuleQProfiles moduleQProfiles, FileSystem fs, AnalysisMode analysisMode) {
    this.moduleQProfiles = moduleQProfiles;
    this.fs = fs;
    this.analysisMode = analysisMode;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    // Should be only executed on leaf modules
    return project.getModules().isEmpty()
      // Useless in issues mode
      && !analysisMode.isIssues();
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    UsedQProfiles used = new UsedQProfiles();
    for (String language : fs.languages()) {
      QProfile profile = moduleQProfiles.findByLanguage(language);
      if (profile != null) {
        used.add(profile);
      }
    }
    Measure<?> detailsMeasure = new Measure<>(CoreMetrics.QUALITY_PROFILES, used.toJson());
    context.saveMeasure(detailsMeasure);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
