package org.sonar.plugins.core.timemachine;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;

import java.util.List;

public class ViolationPersisterDecoratorTest {

  private ViolationPersisterDecorator decorator;

  @Before
  public void setUp() {
    decorator = new ViolationPersisterDecorator(null, null, null);
  }

  @Test
  public void testGetChecksums() {
    List<String> crlf = ViolationPersisterDecorator.getChecksums("Hello\r\nWorld");
    List<String> lf = ViolationPersisterDecorator.getChecksums("Hello\nWorld");
    assertThat(crlf.size(), is(2));
    assertThat(crlf.get(0), not(equalTo(crlf.get(1))));
    assertThat(lf, equalTo(crlf));

    List<String> spaces = ViolationPersisterDecorator.getChecksums("" +
        "\tvoid  method()  {\n" +
        "  void method() {");
    assertThat(spaces.get(0), equalTo(spaces.get(1)));
  }

  @Test
  public void sameRuleLineMessage() {
    Rule rule = Rule.create().setKey("rule");
    Violation violation = Violation.create(rule, null)
        .setLineId(1).setMessage("message");

    RuleFailureModel pastViolation = newPastViolation(rule, 1, "message");

    Multimap<Rule, RuleFailureModel> pastViolationsByRule = LinkedHashMultimap.create();
    pastViolationsByRule.put(rule, pastViolation);

    RuleFailureModel found = decorator.selectPastViolation(violation, pastViolationsByRule);
    assertThat(found, equalTo(pastViolation));
  }

  @Test
  public void differentLine() {
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
  public void differentRule() {
    Rule rule = Rule.create().setKey("rule");
    Violation violation = Violation.create(rule, null)
        .setLineId(1).setMessage("message");

    Rule anotherRule = Rule.create().setKey("anotherRule");
    RuleFailureModel pastViolation = newPastViolation(anotherRule, 1, "message");

    Multimap<Rule, RuleFailureModel> pastViolationsByRule = LinkedHashMultimap.create();
    pastViolationsByRule.put(anotherRule, pastViolation);

    RuleFailureModel found = decorator.selectPastViolation(violation, pastViolationsByRule);
    assertThat(found, nullValue());
  }

  private RuleFailureModel newPastViolation(Rule rule, Integer line, String message) {
    RuleFailureModel pastViolation = new RuleFailureModel();
    pastViolation.setLine(line);
    pastViolation.setMessage(message);
    return pastViolation;
  }

}
