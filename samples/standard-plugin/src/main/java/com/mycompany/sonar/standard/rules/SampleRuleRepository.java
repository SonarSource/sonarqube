package com.mycompany.sonar.standard.rules;

import org.sonar.api.resources.Java;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleRepository;

import java.util.Arrays;
import java.util.List;

/**
 * This class declares rules. It is not the engine used to execute rules during project analysis.
 */
public class SampleRuleRepository extends RuleRepository {

  public static final String REPOSITORY_KEY = "sample";

  public SampleRuleRepository() {
    super(REPOSITORY_KEY, Java.KEY);
    setName("Sample");
  }

  @Override
  public List<Rule> createRules() {
    // This method is called when server is started. It's used to register rules into database.
    // Definition of rules can be loaded from any sources, like XML files.
    return Arrays.asList(
        getRule1(),
        getRule2());
  }

  public Rule getRule1() {
    return Rule.create(REPOSITORY_KEY, "fake1", "Fake one").setSeverity(RulePriority.CRITICAL);
  }

  public Rule getRule2() {
    return Rule.create(REPOSITORY_KEY, "fake2", "Fake two").setDescription("Description of fake two");
  }
}
