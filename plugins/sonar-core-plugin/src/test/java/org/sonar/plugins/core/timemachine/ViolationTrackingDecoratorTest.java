/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.DateUtils;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ViolationTrackingDecoratorTest {

  private ViolationTrackingDecorator decorator;
  private Date analysisDate = DateUtils.parseDate("2010-12-25");

  @Before
  public void setUp() throws ParseException {
    Project project = mock(Project.class);
    when(project.getAnalysisDate()).thenReturn(analysisDate);
    decorator = new ViolationTrackingDecorator(project, null, null);
  }
  
  @Test
  public void checksumShouldHaveGreaterPriorityThanLine() {
    RuleFailureModel referenceViolation1 = newReferenceViolation("message", 1, 50, "checksum1");
    RuleFailureModel referenceViolation2 = newReferenceViolation("message", 3, 50, "checksum2");

    Violation newViolation1 = newViolation("message", 3, 50, "checksum1");
    Violation newViolation2 = newViolation("message", 5, 50, "checksum2");

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(Lists.newArrayList(newViolation1, newViolation2),
        Lists.newArrayList(referenceViolation1, referenceViolation2));
    assertThat(mapping.get(newViolation1), equalTo(referenceViolation1));
    assertThat(mapping.get(newViolation2), equalTo(referenceViolation2));
  }

  @Test
  public void sameRuleAndLineAndChecksumButDifferentMessages() {
    Violation newViolation = newViolation("new message", 1, 50, "checksum1");
    RuleFailureModel referenceViolation = newReferenceViolation("old message", 1, 50, "checksum1");

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(Lists.newArrayList(newViolation), Lists.newArrayList(referenceViolation));
    assertThat(mapping.get(newViolation), equalTo(referenceViolation));
  }

  @Test
  public void sameRuleAndLineMessage() {
    Violation newViolation = newViolation("message", 1, 50, "checksum1");
    RuleFailureModel refernceViolation = newReferenceViolation("message", 1, 50, "checksum2");

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(Lists.newArrayList(newViolation), Lists.newArrayList(refernceViolation));
    assertThat(mapping.get(newViolation), equalTo(refernceViolation));
    assertThat(newViolation.isNew(), is(false));
  }

  @Test
  public void shouldIgnoreReferenceMeasureWithoutChecksum() {
    Violation newViolation = newViolation("message", 1, 50, null);
    RuleFailureModel referenceViolation = newReferenceViolation("message", 1, 51, null);

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(Lists.newArrayList(newViolation), Lists.newArrayList(referenceViolation));
    assertThat(mapping.get(newViolation), is(nullValue()));
  }

  @Test
  public void sameRuleAndMessageAndChecksumButDifferentLine() {
    Violation newViolation = newViolation("message", 1, 50, "checksum1");
    RuleFailureModel referenceViolation = newReferenceViolation("message", 2, 50, "checksum1");

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(Lists.newArrayList(newViolation), Lists.newArrayList(referenceViolation));
    assertThat(mapping.get(newViolation), equalTo(referenceViolation));
  }

  @Test
  public void shouldCreateNewViolationWhenSameRuleSameMessageButDifferentLineAndChecksum() {
    Violation newViolation = newViolation("message", 1, 50, "checksum1");
    RuleFailureModel referenceViolation = newReferenceViolation("message", 2, 50, "checksum2");

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(Lists.newArrayList(newViolation), Lists.newArrayList(referenceViolation));
    assertThat(mapping.get(newViolation), is(nullValue()));
  }

  @Test
  public void shouldNotTrackViolationIfDifferentRule() {
    Violation newViolation = newViolation("message", 1, 50, "checksum1");
    RuleFailureModel referenceViolation = newReferenceViolation("message", 1, 51, "checksum1");

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(Lists.newArrayList(newViolation), Lists.newArrayList(referenceViolation));
    assertThat(mapping.get(newViolation), is(nullValue()));
    assertThat(newViolation.isNew(), is(true));
  }

  @Test
  public void shouldCompareViolationsWithDatabaseFormat() {
    // violation messages are trimmed and can be abbreviated when persisted in database.
    // Comparing violation messages must use the same format.
    Violation newViolation = newViolation("message", 1, 50, "checksum1");
    RuleFailureModel referenceViolation = newReferenceViolation("       message       ", 1, 50, "checksum2");

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(Lists.newArrayList(newViolation), Lists.newArrayList(referenceViolation));
    assertThat(mapping.get(newViolation), equalTo(referenceViolation));
    assertThat(newViolation.isNew(), is(false));
  }

  @Test
  public void shouldSetDateOfNewViolations() {
    Violation newViolation = newViolation("message", 1, 50, "checksum");
    assertThat(newViolation.getCreatedAt(), nullValue());

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(Lists.newArrayList(newViolation), Collections.<RuleFailureModel>emptyList());
    assertThat(mapping.size(), is(0));
    assertThat(newViolation.getCreatedAt(), is(analysisDate));
    assertThat(newViolation.isNew(), is(true));
  }

  @Test
  public void shouldCopyDateWhenNotNew() {
    Violation newViolation = newViolation("message", 1, 50, "checksum");
    RuleFailureModel referenceViolation = newReferenceViolation("", 1, 50, "checksum");
    Date referenceDate = DateUtils.parseDate("2009-05-18");
    referenceViolation.setCreatedAt(referenceDate);
    assertThat(newViolation.getCreatedAt(), nullValue());

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(Lists.newArrayList(newViolation), Lists.<RuleFailureModel>newArrayList(referenceViolation));
    assertThat(mapping.size(), is(1));
    assertThat(newViolation.getCreatedAt(), is(referenceDate));
    assertThat(newViolation.isNew(), is(false));
  }

  private Violation newViolation(String message, int lineId, int ruleId) {
    Rule rule = Rule.create().setKey("rule");
    rule.setId(ruleId);
    return Violation.create(rule, null).setLineId(lineId).setMessage(message);
  }

  private Violation newViolation(String message, int lineId, int ruleId, String lineChecksum) {
    return newViolation(message, lineId, ruleId).setChecksum(lineChecksum);
  }

  private RuleFailureModel newReferenceViolation(String message, int lineId, int ruleId, String lineChecksum) {
    RuleFailureModel referenceViolation = new RuleFailureModel();
    referenceViolation.setId(lineId + ruleId);
    referenceViolation.setLine(lineId);
    referenceViolation.setMessage(message);
    referenceViolation.setRuleId(ruleId);
    referenceViolation.setChecksum(lineChecksum);
    return referenceViolation;
  }

}
