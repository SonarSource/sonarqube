/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.timemachine;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.core.rule.DefaultRuleFinder;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ViolationPersisterDecoratorTest extends AbstractDbUnitTestCase {

  private ViolationPersisterDecorator decorator;
  private Rule rule1 = Rule.create("checkstyle", "com.puppycrawl.tools.checkstyle.checks.header.HeaderCheck", "Check Header");
  private Rule rule2 = Rule.create("checkstyle", "com.puppycrawl.tools.checkstyle.checks.coding.EqualsAvoidNullCheck", "Equals Avoid Null");
  private JavaFile javaFile = new JavaFile("org.foo.Bar");
  Project project = new Project("project");
  private ViolationTrackingDecorator tracker;

  @Before
  public void before() {
    setupData("shared");
    Snapshot snapshot = getSession().getSingleResult(Snapshot.class, "id", 1000);
    ResourcePersister resourcePersister = mock(ResourcePersister.class);
    when(resourcePersister.saveResource(any(Project.class), eq(javaFile))).thenReturn(snapshot);
    when(resourcePersister.getSnapshot(javaFile)).thenReturn(snapshot);
    tracker = mock(ViolationTrackingDecorator.class);
    decorator = new ViolationPersisterDecorator(tracker, resourcePersister, new DefaultRuleFinder(getSessionFactory()), getSession());
  }

  @Test
  public void shouldSaveViolations() {
    Violation violation1a = Violation.create(rule1, javaFile)
        .setSeverity(RulePriority.CRITICAL).setLineId(20).setCost(55.6).setMessage("the message")
        .setChecksum("checksum").setCreatedAt(DateUtils.parseDate("2010-12-25"));
    Violation violation1b = Violation.create(rule1, javaFile)
        .setSeverity(RulePriority.CRITICAL).setLineId(50).setCost(80.0);
    Violation violation2 = Violation.create(rule2, javaFile)
        .setSeverity(RulePriority.MINOR).setSwitchedOff(true);

    decorator.saveViolations(project, Arrays.asList(violation1a, violation1b, violation2));

    checkTables("shouldSaveViolations", "rule_failures");
  }

  @Test
  public void shouldCopyPermanentIdFromReferenceViolation() {
    RuleFailureModel referenceViolation = getSession().getSingleResult(RuleFailureModel.class, "id", 1);
    Violation violation = Violation.create(rule1, javaFile).setSeverity(RulePriority.MAJOR).setMessage("new message");
    when(tracker.getReferenceViolation(violation)).thenReturn(referenceViolation);

    decorator.saveViolations(project, Arrays.asList(violation));

    checkTables("shouldCopyPermanentIdFromReferenceViolation", "rule_failures");
  }
}
