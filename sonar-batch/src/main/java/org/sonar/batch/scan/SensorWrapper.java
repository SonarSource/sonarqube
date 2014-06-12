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
package org.sonar.batch.scan;

import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.resources.Project;
import org.sonar.batch.api.analyzer.Analyzer;
import org.sonar.batch.api.analyzer.AnalyzerContext;
import org.sonar.batch.api.measures.Metric;

import java.util.Arrays;
import java.util.List;

public class SensorWrapper implements Sensor {

  private Analyzer analyzer;
  private AnalyzerContext adaptor;
  private FileSystem fs;

  public SensorWrapper(Analyzer analyzer, AnalyzerContext adaptor, FileSystem fs) {
    this.analyzer = analyzer;
    this.adaptor = adaptor;
    this.fs = fs;
  }

  @DependedUpon
  public List<Metric<?>> provides() {
    return Arrays.asList(analyzer.describe().provides());
  }

  @DependsUpon
  public List<Metric<?>> depends() {
    return Arrays.asList(analyzer.describe().dependsOn());
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    if (!analyzer.describe().languages().isEmpty()) {
      if (project.getLanguageKey() != null && !analyzer.describe().languages().contains(project.getLanguageKey())) {
        return false;
      }
      boolean hasFile = false;
      for (String languageKey : analyzer.describe().languages()) {
        hasFile |= fs.hasFiles(fs.predicates().hasLanguage(languageKey));
      }
      if (!hasFile) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void analyse(Project module, SensorContext context) {
    analyzer.analyse(adaptor);
  }
}
