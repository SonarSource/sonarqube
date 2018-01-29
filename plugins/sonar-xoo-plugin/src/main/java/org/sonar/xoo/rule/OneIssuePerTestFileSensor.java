package org.sonar.xoo.rule;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;

public class OneIssuePerTestFileSensor extends AbstractXooRuleSensor {
  public static final String RULE_KEY = "OneIssuePerTestFile";

  public OneIssuePerTestFileSensor(FileSystem fs, ActiveRules activeRules) {
    super(fs, activeRules);
  }

  @Override
  protected String getRuleKey() {
    return RULE_KEY;
  }

  @Override
  protected void processFile(InputFile inputFile, SensorContext context, RuleKey ruleKey, String languageKey) {
    NewIssue newIssue = context.newIssue();
    newIssue
      .forRule(ruleKey)
      .at(newIssue.newLocation().message("This issue is generated on each test file")
        .on(inputFile))
      .save();
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage(Xoo.KEY)
      .onlyOnFileType(Type.TEST)
      .createIssuesForRuleRepository(XooRulesDefinition.XOO_REPOSITORY);
  }

}
