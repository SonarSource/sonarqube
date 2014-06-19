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
package org.sonar.batch.scan2;

import org.sonar.api.BatchComponent;
import org.sonar.api.batch.analyzer.internal.DefaultAnalyzerDescriptor;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;

public class AnalyzerOptimizer implements BatchComponent {

  private FileSystem fs;

  public AnalyzerOptimizer(FileSystem fs) {
    this.fs = fs;
  }

  /**
   * Decide if the given Analyzer should be executed.
   */
  public boolean shouldExecute(DefaultAnalyzerDescriptor descriptor) {
    FilePredicate predicate = fs.predicates().hasLanguages(descriptor.languages());
    if (descriptor.types().size() == 1) {
      // Size = 0 or Size = 2 means both main and test type
      predicate = fs.predicates().and(
        predicate,
        fs.predicates().hasType(descriptor.types().iterator().next()));
    }
    return fs.hasFiles(predicate);
  }

}
