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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class ViolationPersisterDecoratorTest {

  private ViolationPersisterDecorator decorator;

  @Before
  public void setUp() {
    decorator = new ViolationPersisterDecorator(null, null, null);
  }

  @Test
  public void shouldGenerateCorrectChecksums() {
    List<String> crlf = ViolationPersisterDecorator.getChecksums("Hello\r\nWorld");
    List<String> lf = ViolationPersisterDecorator.getChecksums("Hello\nWorld");
    assertThat(crlf.size(), is(2));
    assertThat(crlf.get(0), not(equalTo(crlf.get(1))));
    assertThat(lf, equalTo(crlf));

    assertThat(ViolationPersisterDecorator.getChecksum("\tvoid  method()  {\n"),
        equalTo(ViolationPersisterDecorator.getChecksum("  void method() {")));
  }

  @Test
  public void sameRuleLineMessage() {
    Rule rule = Rule.create().setKey("rule");
    rule.setId(50);
    Violation violation = Violation.create(rule, null)
        .setLineId(1).setMessage("message");

    RuleFailureModel pastViolation = newPastViolation(rule, 1, "message");

    Multimap<Rule, RuleFailureModel> pastViolationsByRule = LinkedHashMultimap.create();
    pastViolationsByRule.put(rule, pastViolation);

    RuleFailureModel found = decorator.selectPastViolation(violation, pastViolationsByRule);
    assertThat(found, equalTo(pastViolation));
  }

  @Test
  public void sameRuleAndMessageButDifferentLine() {
    Rule rule = Rule.create().setKey("rule");
    rule.setId(50);
    Violation violation = Violation.create(rule, null)
        .setLineId(1).setMessage("message");
    decorator.checksums = ViolationPersisterDecorator.getChecksums("violation");

    RuleFailureModel pastViolation = newPastViolation(rule, 2, "message");
    pastViolation.setChecksum(ViolationPersisterDecorator.getChecksum("violation"));

    Multimap<Rule, RuleFailureModel> pastViolationsByRule = LinkedHashMultimap.create();
    pastViolationsByRule.put(rule, pastViolation);

    RuleFailureModel found = decorator.selectPastViolation(violation, pastViolationsByRule);
    assertThat(found, equalTo(pastViolation));
  }

  @Test
  public void shouldCreateNewViolation() {
    Rule rule = Rule.create().setKey("rule");
    Violation violation = Violation.create(rule, null)
        .setLineId(1).setMessage("message");

    RuleFailureModel pastViolation = newPastViolation(rule, 2, "message");

    Multimap<Rule, RuleFailureModel> pastViolationsByRule = LinkedHashMultimap.create();
    pastViolationsByRule.put(rule, pastViolation);

    RuleFailureModel found = decorator.selectPastViolation(violation, pastViolationsByRule);
    assertThat(found, nullValue());
  }

  @Test
  public void shouldNotTrackViolationIfDifferentRule() {
    Rule rule = Rule.create().setKey("rule");
    rule.setId(50);
    Violation violation = Violation.create(rule, null)
        .setLineId(1).setMessage("message");

    Rule otherRule = Rule.create().setKey("anotherRule");
    otherRule.setId(244);
    RuleFailureModel pastViolationOnOtherRule = newPastViolation(otherRule, 1, "message");

    Multimap<Rule, RuleFailureModel> pastViolationsByRule = LinkedHashMultimap.create();
    pastViolationsByRule.put(otherRule, pastViolationOnOtherRule);

    RuleFailureModel found = decorator.selectPastViolation(violation, pastViolationsByRule);
    assertThat(found, nullValue());
  }

  @Test
  public void shouldCompareViolationsWithDatabaseFormat() {
    // violation messages are trimmed and can be abbreviated when persisted in database.
    // Comparing violation messages must use the same format.
    Rule rule = Rule.create().setKey("rule");
    rule.setId(50);
    Violation violation = Violation.create(rule, null)
        .setLineId(30).setMessage("   message     "); // starts and ends with whitespaces

    RuleFailureModel pastViolation = newPastViolation(rule, 30, "message"); // trimmed in database

    Multimap<Rule, RuleFailureModel> pastViolationsByRule = LinkedHashMultimap.create();
    pastViolationsByRule.put(rule, pastViolation);

    RuleFailureModel found = decorator.selectPastViolation(violation, pastViolationsByRule);
    assertThat(found, equalTo(pastViolation));
  }

  private RuleFailureModel newPastViolation(Rule rule, Integer line, String message) {
    RuleFailureModel pastViolation = new RuleFailureModel();
    pastViolation.setLine(line);
    pastViolation.setMessage(message);
    pastViolation.setRuleId(rule.getId());
    return pastViolation;
  }

}
