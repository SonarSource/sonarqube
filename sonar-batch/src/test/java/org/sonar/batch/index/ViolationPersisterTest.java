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
package org.sonar.batch.index;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;
import org.sonar.core.components.DefaultRuleFinder;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ViolationPersisterTest extends AbstractDbUnitTestCase {

  private ViolationPersister violationPersister;
  private Rule rule1 = Rule.create("checkstyle", "com.puppycrawl.tools.checkstyle.checks.header.HeaderCheck", "Check Header");
  private Rule rule2 = Rule.create("checkstyle", "com.puppycrawl.tools.checkstyle.checks.coding.EqualsAvoidNullCheck", "Equals Avoid Null");
  private JavaFile javaFile = new JavaFile("org.foo.Bar");

  @Before
  public void before() {
    setupData("shared");
    Snapshot snapshot = getSession().getSingleResult(Snapshot.class, "id", 1000);
    ResourcePersister resourcePersister = mock(ResourcePersister.class);
    when(resourcePersister.saveResource((Project) anyObject(), eq(javaFile))).thenReturn(snapshot);
    when(resourcePersister.getLastSnapshot(snapshot, true)).thenReturn(snapshot);
    when(resourcePersister.getSnapshot(javaFile)).thenReturn(snapshot);
    violationPersister = new ViolationPersister(getSession(), resourcePersister, new DefaultRuleFinder(getSessionFactory()));
  }

  @Test
  public void shouldSaveViolations() {
    Violation violation1a = Violation.create(rule1, javaFile)
        .setPriority(RulePriority.CRITICAL).setLineId(20).setCost(55.6)
        .setMessage("the message");
    Violation violation1b = Violation.create(rule1, javaFile)
        .setPriority(RulePriority.CRITICAL).setLineId(50).setCost(80.0);
    Violation violation2 = Violation.create(rule2, javaFile)
        .setPriority(RulePriority.MINOR);
    Project project = new Project("project");

    violationPersister.saveViolation(project, violation1a);
    violationPersister.saveViolation(project, violation1b);
    violationPersister.saveViolation(project, violation2);

    checkTables("shouldInsertViolations", "rule_failures");
  }

  @Test
  public void shouldUpdateViolation() {
    Violation violation = Violation.create(rule1, javaFile)
        .setLineId(20).setCost(55.6);
    RuleFailureModel model = getSession().getSingleResult(RuleFailureModel.class, "id", 1);

    violationPersister.saveOrUpdateViolation(new Project("project"), violation, model, null);

    assertThat(violation.getCreatedAt(), notNullValue());
    checkTables("shouldUpdateViolation", "rule_failures");
  }
}
