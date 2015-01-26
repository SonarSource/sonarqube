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
package org.sonar.server.computation;

import org.sonar.core.issue.db.UpdateConflictResolver;
import org.sonar.server.computation.issue.IssueCache;
import org.sonar.server.computation.issue.IssueComputation;
import org.sonar.server.computation.issue.RuleCache;
import org.sonar.server.computation.issue.RuleCacheLoader;
import org.sonar.server.computation.issue.ScmAccountCache;
import org.sonar.server.computation.issue.ScmAccountCacheLoader;
import org.sonar.server.computation.issue.SourceLinesCache;
import org.sonar.server.computation.step.ComputationSteps;

import java.util.Arrays;
import java.util.List;

public class ComputationComponents {

  private ComputationComponents() {
    // only static stuff
  }

  /**
   * List of all objects to be injected in the picocontainer dedicated to computation stack. 
   * Does not contain the steps declared in {@link org.sonar.server.computation.step.ComputationSteps#orderedStepClasses()}.
   */
  public static List nonStepComponents() {
    return Arrays.asList(
      ComputationService.class,
      ComputationSteps.class,

      // issues
      ScmAccountCacheLoader.class,
      ScmAccountCache.class,
      SourceLinesCache.class,
      IssueComputation.class,
      RuleCache.class,
      RuleCacheLoader.class,
      IssueCache.class,
      UpdateConflictResolver.class);
  }
}
