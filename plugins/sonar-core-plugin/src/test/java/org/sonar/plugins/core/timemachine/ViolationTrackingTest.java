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
package org.sonar.plugins.core.timemachine;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.scan.LastSnapshots;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ViolationTrackingTest {

  private final Date analysisDate = DateUtils.parseDate("2010-12-25");

  private ViolationTrackingDecorator decorator;

  private Project project;
  private LastSnapshots lastSnapshots;

  @Before
  public void setUp() {
    project = mock(Project.class);
    when(project.getAnalysisDate()).thenReturn(analysisDate);
    lastSnapshots = mock(LastSnapshots.class);
    decorator = new ViolationTrackingDecorator(project, lastSnapshots, null);
  }

  @Test
  public void pastViolationNotAssiciatedWithLineShouldNotCauseNPE() throws Exception {
    when(lastSnapshots.getSource(project)).thenReturn(load("example2-v1"));
    String source = load("example2-v2");

    RuleFailureModel referenceViolation1 = newReferenceViolation("2 branches need to be covered", null, 50);

    Violation newViolation1 = newViolation("Indentation", 9, 50);
    newViolation1.setChecksum("foo");

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(
        Arrays.asList(newViolation1),
        Arrays.asList(referenceViolation1),
        source, project);

    assertThat(mapping.isEmpty(), is(true));
    assertThat(newViolation1.isNew(), is(true));
  }

  @Test
  public void newViolationNotAssiciatedWithLineShouldNotCauseNPE() throws Exception {
    when(lastSnapshots.getSource(project)).thenReturn(load("example2-v1"));
    String source = load("example2-v2");

    RuleFailureModel referenceViolation1 = newReferenceViolation("Indentation", 7, 50);

    Violation newViolation1 = newViolation("1 branch need to be covered", null, 50);
    newViolation1.setChecksum("foo");

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(
        Arrays.asList(newViolation1),
        Arrays.asList(referenceViolation1),
        source, project);

    assertThat(mapping.isEmpty(), is(true));
    assertThat(newViolation1.isNew(), is(true));
  }

  /**
   * SONAR-2928
   */
  @Test
  public void violationNotAssociatedWithLine() throws Exception {
    when(lastSnapshots.getSource(project)).thenReturn(load("example2-v1"));
    String source = load("example2-v2");

    RuleFailureModel referenceViolation1 = newReferenceViolation("2 branches need to be covered", null, 50);

    Violation newViolation1 = newViolation("1 branch need to be covered", null, 50);

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(
        Arrays.asList(newViolation1),
        Arrays.asList(referenceViolation1),
        source, project);

    assertThat(newViolation1.isNew(), is(false));
    assertThat(mapping.get(newViolation1), equalTo(referenceViolation1));
  }

  /**
   * SONAR-3072
   */
  @Test
  public void example1() throws Exception {
    when(lastSnapshots.getSource(project)).thenReturn(load("example1-v1"));
    String source = load("example1-v2");

    RuleFailureModel referenceViolation1 = newReferenceViolation("Indentation", 7, 50);
    RuleFailureModel referenceViolation2 = newReferenceViolation("Indentation", 11, 50);

    Violation newViolation1 = newViolation("Indentation", 9, 50);
    Violation newViolation2 = newViolation("Indentation", 13, 50);
    Violation newViolation3 = newViolation("Indentation", 17, 50);
    Violation newViolation4 = newViolation("Indentation", 21, 50);

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(
        Arrays.asList(newViolation1, newViolation2, newViolation3, newViolation4),
        Arrays.asList(referenceViolation1, referenceViolation2),
        source, project);

    assertThat(newViolation1.isNew(), is(true));
    assertThat(newViolation2.isNew(), is(true));
    assertThat(newViolation3.isNew(), is(false));
    assertThat(mapping.get(newViolation3), equalTo(referenceViolation1));
    assertThat(newViolation4.isNew(), is(false));
    assertThat(mapping.get(newViolation4), equalTo(referenceViolation2));
  }

  /**
   * SONAR-3072
   */
  @Test
  public void example2() throws Exception {
    when(lastSnapshots.getSource(project)).thenReturn(load("example2-v1"));
    String source = load("example2-v2");

    RuleFailureModel referenceViolation1 = newReferenceViolation("SystemPrintln", 5, 50);

    Violation newViolation1 = newViolation("SystemPrintln", 6, 50);
    Violation newViolation2 = newViolation("SystemPrintln", 10, 50);
    Violation newViolation3 = newViolation("SystemPrintln", 14, 50);

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(
        Arrays.asList(newViolation1, newViolation2, newViolation3),
        Arrays.asList(referenceViolation1),
        source, project);

    assertThat(newViolation1.isNew(), is(true));
    assertThat(newViolation2.isNew(), is(false));
    assertThat(mapping.get(newViolation2), equalTo(referenceViolation1));
    assertThat(newViolation3.isNew(), is(true));
  }

  private Violation newViolation(String message, Integer lineId, int ruleId) {
    Rule rule = Rule.create().setKey("rule");
    rule.setId(ruleId);
    return Violation.create(rule, null).setLineId(lineId).setMessage(message);
  }

  private RuleFailureModel newReferenceViolation(String message, Integer lineId, int ruleId) {
    RuleFailureModel referenceViolation = new RuleFailureModel();
    referenceViolation.setId(violationId++);
    referenceViolation.setLine(lineId);
    referenceViolation.setMessage(message);
    referenceViolation.setRuleId(ruleId);
    return referenceViolation;
  }

  private int violationId = 0;

  private static String load(String name) throws IOException {
    return Resources.toString(ViolationTrackingTest.class.getResource("ViolationTrackingTest/" + name + ".txt"), Charsets.UTF_8);
  }

}
