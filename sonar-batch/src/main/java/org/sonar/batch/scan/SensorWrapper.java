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
import org.sonar.api.batch.analyzer.Analyzer;
import org.sonar.api.batch.analyzer.AnalyzerContext;
import org.sonar.api.batch.analyzer.internal.DefaultAnalyzerDescriptor;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.measures.Metric;
import org.sonar.api.resources.Project;

import java.util.Arrays;
import java.util.List;

public class SensorWrapper implements Sensor {

  private Analyzer analyzer;
  private AnalyzerContext adaptor;
  private FileSystem fs;
  private DefaultAnalyzerDescriptor descriptor;

  public SensorWrapper(Analyzer analyzer, AnalyzerContext adaptor, FileSystem fs) {
    this.analyzer = analyzer;
    descriptor = new DefaultAnalyzerDescriptor();
    analyzer.describe(descriptor);
    this.adaptor = adaptor;
    this.fs = fs;
  }

  @DependedUpon
  public List<Metric<?>> provides() {
    return Arrays.asList(descriptor.provides());
  }

  @DependsUpon
  public List<Metric<?>> depends() {
    return Arrays.asList(descriptor.dependsOn());
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    if (!descriptor.languages().isEmpty()) {
      if (project.getLanguageKey() != null && !descriptor.languages().contains(project.getLanguageKey())) {
        return false;
      }
      boolean hasFile = false;
      for (String languageKey : descriptor.languages()) {
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
