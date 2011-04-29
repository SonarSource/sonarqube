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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;

import com.google.common.collect.Lists;

public class ViolationPersisterDecoratorTest {

  private ViolationPersisterDecorator decorator;

  @Before
  public void setUp() {
    decorator = new ViolationPersisterDecorator(null, null);
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2358
   */
  @Test
  public void shouldGenerateCorrectChecksums() {
    List<String> encoding = ViolationPersisterDecorator.getChecksums("Привет Мир");
    assertThat(encoding.size(), is(1));
    assertThat(encoding.get(0), is("5ba3a45e1299ede07f56e5531351be52"));
  }

  @Test
  public void shouldSplitLinesAndIgnoreSpaces() {
    List<String> crlf = ViolationPersisterDecorator.getChecksums("Hello\r\nWorld");
    List<String> lf = ViolationPersisterDecorator.getChecksums("Hello\nWorld");
    List<String> cr = ViolationPersisterDecorator.getChecksums("Hello\rWorld");
    assertThat(crlf.size(), is(2));
    assertThat(crlf.get(0), not(equalTo(crlf.get(1))));
    assertThat(lf, equalTo(crlf));
    assertThat(cr, equalTo(crlf));

    assertThat(ViolationPersisterDecorator.getChecksum("\tvoid  method()  {\n"),
        equalTo(ViolationPersisterDecorator.getChecksum("  void method() {")));
  }

  @Test
  public void checksumShouldHaveGreaterPriorityThanLine() {
    RuleFailureModel pastViolation1 = newPastViolation("message", 1, 50, "checksum1");
    RuleFailureModel pastViolation2 = newPastViolation("message", 3, 50, "checksum2");

    Violation newViolation1 = newViolation("message", 3, 50, "checksum1");
    Violation newViolation2 = newViolation("message", 5, 50, "checksum2");

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(Lists.newArrayList(newViolation1, newViolation2),
        Lists.newArrayList(pastViolation1, pastViolation2));
    assertThat(mapping.get(newViolation1), equalTo(pastViolation1));
    assertThat(mapping.get(newViolation2), equalTo(pastViolation2));
  }

  @Test
  public void sameRuleAndLineMessage() {
    Violation newViolation = newViolation("message", 1, 50, "checksum1");
    RuleFailureModel pastViolation = newPastViolation("message", 1, 50, "checksum2");

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(Lists.newArrayList(newViolation), Lists.newArrayList(pastViolation));
    assertThat(mapping.get(newViolation), equalTo(pastViolation));
  }

  @Test
  public void pastMeasureHasNoChecksum() {
    Violation newViolation = newViolation("message", 1, 50, null);
    RuleFailureModel pastViolation = newPastViolation("message", 1, 51, null);

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(Lists.newArrayList(newViolation), Lists.newArrayList(pastViolation));
    assertThat(mapping.get(newViolation), is(nullValue()));
  }

  @Test
  public void sameRuleAndMessageAndChecksumButDifferentLine() {
    Violation newViolation = newViolation("message", 1, 50, "checksum1");
    RuleFailureModel pastViolation = newPastViolation("message", 2, 50, "checksum1");

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(Lists.newArrayList(newViolation), Lists.newArrayList(pastViolation));
    assertThat(mapping.get(newViolation), equalTo(pastViolation));
  }

  @Test
  public void shouldCreateNewViolationWhenSameRuleSameMessageButDifferentLineAndChecksum() {
    Violation newViolation = newViolation("message", 1, 50, "checksum1");
    RuleFailureModel pastViolation = newPastViolation("message", 2, 50, "checksum2");

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(Lists.newArrayList(newViolation), Lists.newArrayList(pastViolation));
    assertThat(mapping.get(newViolation), is(nullValue()));
  }

  @Test
  public void shouldNotTrackViolationIfDifferentRule() {
    Violation newViolation = newViolation("message", 1, 50, "checksum1");
    RuleFailureModel pastViolation = newPastViolation("message", 1, 51, "checksum1");

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(Lists.newArrayList(newViolation), Lists.newArrayList(pastViolation));
    assertThat(mapping.get(newViolation), is(nullValue()));
  }

  @Test
  public void shouldCompareViolationsWithDatabaseFormat() {
    // violation messages are trimmed and can be abbreviated when persisted in database.
    // Comparing violation messages must use the same format.
    Violation newViolation = newViolation("message", 1, 50, "checksum1");
    RuleFailureModel pastViolation = newPastViolation("       message       ", 1, 50, "checksum2");

    Map<Violation, RuleFailureModel> mapping = decorator.mapViolations(Lists.newArrayList(newViolation), Lists.newArrayList(pastViolation));
    assertThat(mapping.get(newViolation), equalTo(pastViolation));
  }

  private Violation newViolation(String message, int lineId, int ruleId) {
    Rule rule = Rule.create().setKey("rule");
    rule.setId(ruleId);
    return Violation.create(rule, null).setLineId(lineId).setMessage(message);
  }

  private Violation newViolation(String message, int lineId, int ruleId, String lineChecksum) {
    Violation violation = newViolation(message, lineId, ruleId);
    if (decorator.checksums == null) {
      decorator.checksums = Lists.newArrayListWithExpectedSize(100);
    }
    for (int i = decorator.checksums.size() - 1; i < lineId; i++) {
      decorator.checksums.add("");
    }
    if (lineChecksum != null) {
      decorator.checksums.set(lineId - 1, ViolationPersisterDecorator.getChecksum(lineChecksum));
    }
    return violation;
  }

  private RuleFailureModel newPastViolation(String message, int lineId, int ruleId) {
    RuleFailureModel pastViolation = new RuleFailureModel();
    pastViolation.setId(lineId + ruleId);
    pastViolation.setLine(lineId);
    pastViolation.setMessage(message);
    pastViolation.setRuleId(ruleId);
    return pastViolation;
  }

  private RuleFailureModel newPastViolation(String message, int lineId, int ruleId, String lineChecksum) {
    RuleFailureModel pastViolation = newPastViolation(message, lineId, ruleId);
    if (lineChecksum != null) {
      pastViolation.setChecksum(ViolationPersisterDecorator.getChecksum(lineChecksum));
    }
    return pastViolation;
  }

}
