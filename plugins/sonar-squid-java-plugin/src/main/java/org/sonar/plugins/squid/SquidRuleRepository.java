package org.sonar.plugins.squid;

import org.sonar.api.resources.Java;
import org.sonar.api.rules.AnnotationRuleParser;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleRepository;
import org.sonar.java.bytecode.check.BytecodeChecks;

import java.util.List;

public final class SquidRuleRepository extends RuleRepository {
  private AnnotationRuleParser ruleParser;

  public SquidRuleRepository() {
    super(SquidConstants.REPOSITORY_KEY, Java.KEY);
    setName(SquidConstants.REPOSITORY_NAME);
    this.ruleParser = new AnnotationRuleParser(); // TODO bug?
  }

  @Override
  public List<Rule> createRules() {
    return ruleParser.parse(SquidConstants.REPOSITORY_KEY, BytecodeChecks.getCheckClasses());
  }
}
