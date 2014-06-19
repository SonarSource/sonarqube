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
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;

public class AnalyzerOptimizer implements BatchComponent {

  private final FileSystem fs;
  private final ActiveRules activeRules;

  public AnalyzerOptimizer(FileSystem fs, ActiveRules activeRules) {
    this.fs = fs;
    this.activeRules = activeRules;
  }

  /**
   * Decide if the given Analyzer should be executed.
   */
  public boolean shouldExecute(DefaultAnalyzerDescriptor descriptor) {
    // FS Conditions
    boolean fsCondition = fsCondition(descriptor);
    boolean activeRulesCondition = activeRulesCondition(descriptor);
    return fsCondition && activeRulesCondition;
  }

  private boolean activeRulesCondition(DefaultAnalyzerDescriptor descriptor) {
    if (!descriptor.ruleRepositories().isEmpty()) {
      for (String repoKey : descriptor.ruleRepositories()) {
        if (!activeRules.findByRepository(repoKey).isEmpty()) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

  private boolean fsCondition(DefaultAnalyzerDescriptor descriptor) {
    if (!descriptor.languages().isEmpty() || !descriptor.types().isEmpty()) {
      FilePredicate langPredicate = descriptor.languages().isEmpty() ? fs.predicates().all() : fs.predicates().hasLanguages(descriptor.languages());

      FilePredicate typePredicate = descriptor.types().isEmpty() ? fs.predicates().all() : fs.predicates().none();
      for (InputFile.Type type : descriptor.types()) {
        typePredicate = fs.predicates().or(
          typePredicate,
          fs.predicates().hasType(type));
      }
      return fs.hasFiles(fs.predicates().and(langPredicate, typePredicate));
    }
    return true;
  }

}
