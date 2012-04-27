/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.pmd;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import net.sourceforge.pmd.IRuleViolation;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;

import java.io.File;
import java.util.List;

public class PmdViolationToRuleViolation implements BatchExtension {
  private final ProjectFileSystem projectFileSystem;
  private final RuleFinder ruleFinder;

  public PmdViolationToRuleViolation(ProjectFileSystem projectFileSystem, RuleFinder ruleFinder) {
    this.projectFileSystem = projectFileSystem;
    this.ruleFinder = ruleFinder;
  }

  public Violation toViolation(IRuleViolation pmdViolation, SensorContext context) {
    Resource resource = findResourceFor(pmdViolation);
    if (context.getResource(resource) == null) {
      return null; // Save violations only for existing resources
    }

    Rule rule = findRuleFor(pmdViolation);
    if (rule == null) {
      return null; // Save violations only for enabled rules
    }

    int lineId = pmdViolation.getBeginLine();
    String message = pmdViolation.getDescription();

    return Violation.create(rule, resource).setLineId(lineId).setMessage(message);
  }

  private Resource findResourceFor(IRuleViolation violation) {
    List<File> allSources = ImmutableList.copyOf(Iterables.concat(projectFileSystem.getSourceDirs(), projectFileSystem.getTestDirs()));

    return JavaFile.fromAbsolutePath(violation.getFilename(), allSources, true);
  }

  private Rule findRuleFor(IRuleViolation violation) {
    String ruleKey = violation.getRule().getName();
    Rule rule = ruleFinder.findByKey(PmdConstants.REPOSITORY_KEY, ruleKey);
    if (rule != null) {
      return rule;
    }
    return ruleFinder.findByKey(PmdConstants.TEST_REPOSITORY_KEY, ruleKey);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
