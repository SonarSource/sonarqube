package com.mycompany.sonar.checkstyle;

import org.apache.commons.io.IOUtils;
import org.sonar.api.resources.Java;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.rules.XMLRuleParser;

import java.io.InputStream;
import java.util.List;

public class CheckstyleExtensionRepository extends RuleRepository {

  // Must be the same than the Checkstyle plugin
  private static final String REPOSITORY_KEY = "checkstyle";
  private XMLRuleParser ruleParser;

  public CheckstyleExtensionRepository(XMLRuleParser ruleParser) {
    super(REPOSITORY_KEY, Java.KEY);
    this.ruleParser = ruleParser;
  }

  @Override
  public List<Rule> createRules() {
    // In this example, new rules are declared in a XML file
    InputStream input = getClass().getResourceAsStream("/com/mycompany/sonar/checkstyle/extensions.xml");
    try {
      return ruleParser.parse(input);
      
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

}
