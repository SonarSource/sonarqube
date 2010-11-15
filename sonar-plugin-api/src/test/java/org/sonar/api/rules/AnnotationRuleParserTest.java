package org.sonar.api.rules;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.sonar.check.IsoCategory;
import org.sonar.check.Priority;

public class AnnotationRuleParserTest {

  @Test
  public void ruleWithProperty() {
    List<Rule> rules = parseAnnotatedClass(RuleWithProperty.class);
    assertThat(rules.size(), is(1));
    Rule rule = rules.get(0);
    assertThat(rule.getKey(), is("foo"));
    assertThat(rule.getName(), is("bar"));
    assertThat(rule.getPriority(), is(RulePriority.BLOCKER));
    assertThat(rule.getRulesCategory(), is(Iso9126RulesCategories.MAINTAINABILITY));
    assertThat(rule.getParams().size(), is(1));
    RuleParam prop = rule.getParam("property");
    assertThat(prop.getKey(), is("property"));
    assertThat(prop.getDescription(), is("Ignore ?"));
    assertThat(prop.getDefaultValue(), is("false"));
  }

  @Test
  public void ruleWithoutName() {
    List<Rule> rules = parseAnnotatedClass(RuleWithoutName.class);
    assertThat(rules.size(), is(1));
    Rule rule = rules.get(0);
    assertThat(rule.getKey(), is("foo"));
    assertThat(rule.getName(), is("foo"));
    assertThat(rule.getPriority(), is(RulePriority.MAJOR));
    assertThat(rule.getRulesCategory(), is(Iso9126RulesCategories.MAINTAINABILITY));
  }

  @Test
  public void ruleWithoutKey() {
    List<Rule> rules = parseAnnotatedClass(RuleWithoutKey.class);
    assertThat(rules.size(), is(1));
    Rule rule = rules.get(0);
    assertThat(rule.getKey(), is(RuleWithoutKey.class.getCanonicalName()));
    assertThat(rule.getName(), is("foo"));
    assertThat(rule.getPriority(), is(RulePriority.MAJOR));
    assertThat(rule.getRulesCategory(), is(Iso9126RulesCategories.MAINTAINABILITY));
  }

  @Test
  public void supportDeprecatedAnnotations() {
    List<Rule> rules = parseAnnotatedClass(Check.class);
    assertThat(rules.size(), is(1));
    Rule rule = rules.get(0);
    assertThat(rule.getKey(), is(Check.class.getCanonicalName()));
    assertThat(rule.getName(), is(Check.class.getCanonicalName()));
    assertThat(rule.getDescription(), is("Deprecated check"));
    assertThat(rule.getPriority(), is(RulePriority.BLOCKER));
    assertThat(rule.getRulesCategory(), is(Iso9126RulesCategories.MAINTAINABILITY));
  }

  private List<Rule> parseAnnotatedClass(Class annotatedClass) {
    return new AnnotationRuleParser().parse("repo", Collections.singleton(annotatedClass));
  }

  @org.sonar.check.Rule(name = "foo", isoCategory = IsoCategory.Maintainability)
  private class RuleWithoutKey {
  }

  @org.sonar.check.Rule(key = "foo", isoCategory = IsoCategory.Maintainability)
  private class RuleWithoutName {
  }

  @org.sonar.check.Rule(key = "foo", name = "bar", isoCategory = IsoCategory.Maintainability, priority = Priority.BLOCKER)
  private class RuleWithProperty {
    @org.sonar.check.RuleProperty(description = "Ignore ?", defaultValue = "false")
    String property;
  }

  @org.sonar.check.Check(description = "Deprecated check", isoCategory = IsoCategory.Maintainability, priority = Priority.BLOCKER)
  private class Check {
  }

}
