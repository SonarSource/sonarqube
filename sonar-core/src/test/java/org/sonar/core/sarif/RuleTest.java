/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import java.util.Set;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.sarif.PropertiesBag;
import org.sonar.core.sarif.Rule;

import static org.assertj.core.api.Assertions.assertThat;


public class RuleTest {

  @Test
  public void equals_matchOnlyOnId() {
    Rule rule1 = createRule("rep1", "rule1");
    Rule rule1Bis = createRule("rep1", "rule1");
    Rule rule2 = withRuleId(rule1, "rep1", "rule2");

    assertThat(rule1).isEqualTo(rule1Bis).isNotEqualTo(rule2);
  }

  @Test
  public void equals_notMatchWithNull(){
    Rule rule1 = createRule("rep1", "rule2");

    assertThat(rule1).isNotEqualTo(null);
  }

  @Test
  public void equals_matchWithSameObject(){
    Rule rule1 = createRule("rep5", "rule2");

    assertThat(rule1).isEqualTo(rule1);
  }

  private static Rule withRuleId(Rule rule, String repoName, String ruleName) {
    return new Rule(RuleKey.of(repoName, ruleName), rule.getName(), rule.getFullDescription().getText(), rule.getProperties());
  }

  private static Rule createRule(String repoName, String ruleName) {
    return new Rule(RuleKey.of(repoName, ruleName), RandomStringUtils.randomAlphanumeric(5), RandomStringUtils.randomAlphanumeric(5),
      PropertiesBag.of(RandomStringUtils.randomAlphanumeric(3), Set.of(RandomStringUtils.randomAlphanumeric(4))));
  }

}
