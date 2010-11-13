package org.sonar.plugins.squid;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.resources.Java;
import org.sonar.api.rules.AnnotationRuleParser;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleRepository;
import org.sonar.java.ast.check.BreakCheck;
import org.sonar.java.ast.check.ContinueCheck;
import org.sonar.java.ast.check.UndocumentedApiCheck;
import org.sonar.java.bytecode.check.ArchitectureCheck;
import org.sonar.java.bytecode.check.CallToDeprecatedMethodCheck;
import org.sonar.java.bytecode.check.DITCheck;
import org.sonar.java.bytecode.check.UnusedPrivateMethodCheck;
import org.sonar.java.bytecode.check.UnusedProtectedMethodCheck;

public final class SquidRuleRepository extends RuleRepository {
  private AnnotationRuleParser ruleParser;

  public SquidRuleRepository(AnnotationRuleParser ruleParser) {
    super(SquidConstants.REPOSITORY_KEY, Java.KEY);
    setName(SquidConstants.REPOSITORY_NAME);
    this.ruleParser = ruleParser;
  }

  @Override
  public List<Rule> createRules() {
    return ruleParser.parse(SquidConstants.REPOSITORY_KEY, getCheckClasses());
  }

  public static List<Class> getCheckClasses() {
    return Arrays.asList(
        // Bytecode checks
        (Class) CallToDeprecatedMethodCheck.class, UnusedPrivateMethodCheck.class, UnusedProtectedMethodCheck.class,
        ArchitectureCheck.class, DITCheck.class,
        // AST checks
        UndocumentedApiCheck.class, ContinueCheck.class, BreakCheck.class);
  }
}
