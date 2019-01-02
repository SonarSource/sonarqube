/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.xoo.rule;

import javax.annotation.Nullable;
import org.sonar.api.SonarRuntime;
import org.sonar.api.rule.RuleScope;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionAnnotationLoader;
import org.sonar.api.utils.Version;
import org.sonar.xoo.Xoo;
import org.sonar.xoo.Xoo2;
import org.sonar.xoo.checks.Check;

/**
 * Define all the coding rules that are supported on the repositories named "xoo" and "xoo2"
 */
public class XooRulesDefinition implements RulesDefinition {

  public static final String XOO_REPOSITORY = "xoo";
  public static final String XOO2_REPOSITORY = "xoo2";

  private static final String TEN_MIN = "10min";

  @Nullable
  private final Version version;

  public XooRulesDefinition() {
    this(null);
  }

  public XooRulesDefinition(@Nullable SonarRuntime sonarRuntime) {
    this.version = sonarRuntime != null ? sonarRuntime.getApiVersion() : null;
  }

  @Override
  public void define(Context context) {
    defineRulesXoo(context);
    defineRulesXoo2(context);
    defineRulesXooExternal(context);
  }

  private static void defineRulesXoo2(Context context) {
    NewRepository repo = context.createRepository(XOO2_REPOSITORY, Xoo2.KEY).setName("Xoo2");

    NewRule hasTag = repo.createRule(HasTagSensor.RULE_KEY).setName("Has Tag")
      .setHtmlDescription("Search for a given tag in Xoo files");

    NewRule oneIssuePerLine = repo.createRule(OneIssuePerLineSensor.RULE_KEY).setName("One Issue Per Line")
      .setHtmlDescription("Generate an issue on each line of a file. It requires the metric \"lines\".");
    oneIssuePerLine
      .setDebtRemediationFunction(hasTag.debtRemediationFunctions().linear("1min"))
      .setGapDescription("It takes about 1 minute to an experienced software craftsman to remove a line of code");

    repo.done();
  }

  private void defineRulesXoo(Context context) {
    NewRepository repo = context.createRepository(XOO_REPOSITORY, Xoo.KEY).setName("Xoo");

    new RulesDefinitionAnnotationLoader().load(repo, Check.ALL);

    NewRule hasTag = repo.createRule(HasTagSensor.RULE_KEY).setName("Has Tag")
      .setActivatedByDefault(true)
      .setHtmlDescription("Search for a given tag in Xoo files");
    hasTag
      .setDebtRemediationFunction(hasTag.debtRemediationFunctions().constantPerIssue("2min"));
    hasTag.createParam("tag")
      .setDefaultValue("xoo")
      .setDescription("The tag to search for");

    NewRule ruleWithParameters = repo.createRule("RuleWithParameters").setName("Rule with parameters")
      .setHtmlDescription("Rule containing parameter of different types : boolean, integer, etc. For information, no issue will be linked to this rule.");
    ruleWithParameters.createParam("string").setType(RuleParamType.STRING);
    ruleWithParameters.createParam("text").setType(RuleParamType.TEXT);
    ruleWithParameters.createParam("boolean").setType(RuleParamType.BOOLEAN);
    ruleWithParameters.createParam("integer").setType(RuleParamType.INTEGER);
    ruleWithParameters.createParam("float").setType(RuleParamType.FLOAT);

    NewRule oneIssuePerLine = repo.createRule(OneIssuePerLineSensor.RULE_KEY).setName("One Issue Per Line")
      .setHtmlDescription("Generate an issue on each line of a file. It requires the metric \"lines\".");
    oneIssuePerLine
      .setDebtRemediationFunction(oneIssuePerLine.debtRemediationFunctions().linear("1min"))
      .setGapDescription("It takes about 1 minute to an experienced software craftsman to remove a line of code");

    repo.createRule(OneIssueOnDirPerFileSensor.RULE_KEY).setName("One Issue On Dir Per File")
      .setHtmlDescription("Generate issues on directories");

    NewRule oneIssuePerFile = repo.createRule(OneIssuePerFileSensor.RULE_KEY).setName("One Issue Per File")
      .setHtmlDescription("Generate an issue on each file");
    oneIssuePerFile.setDebtRemediationFunction(oneIssuePerFile.debtRemediationFunctions().linear(TEN_MIN));

    NewRule oneIssuePerTestFile = repo.createRule(OneIssuePerTestFileSensor.RULE_KEY).setName("One Issue Per Test File")
      .setScope(RuleScope.TEST)
      .setHtmlDescription("Generate an issue on each test file");
    oneIssuePerTestFile.setDebtRemediationFunction(oneIssuePerTestFile.debtRemediationFunctions().linear(TEN_MIN));

    NewRule oneIssuePerDirectory = repo.createRule(OneIssuePerDirectorySensor.RULE_KEY).setName("One Issue Per Directory")
      .setHtmlDescription("Generate an issue on each non-empty directory");
    oneIssuePerDirectory.setDebtRemediationFunction(oneIssuePerDirectory.debtRemediationFunctions().linear(TEN_MIN));

    NewRule oneDayDebtPerFile = repo.createRule(OneDayDebtPerFileSensor.RULE_KEY).setName("One Day Debt Per File")
      .setHtmlDescription("Generate an issue on each file with a debt of one day");
    oneDayDebtPerFile.setDebtRemediationFunction(oneDayDebtPerFile.debtRemediationFunctions().linear("1d"));

    NewRule oneIssuePerModule = repo.createRule(OneIssuePerModuleSensor.RULE_KEY).setName("One Issue Per Module")
      .setHtmlDescription("Generate an issue on each module");
    oneIssuePerModule
      .setDebtRemediationFunction(oneIssuePerModule.debtRemediationFunctions().linearWithOffset("25min", "1h"))
      .setGapDescription("A certified architect will need roughly half an hour to start working on removal of modules, " +
        "then it's about one hour per module.");

    repo.createRule(OneBlockerIssuePerFileSensor.RULE_KEY).setName("One Blocker Issue Per File")
      .setHtmlDescription("Generate a blocker issue on each file, whatever the severity declared in the Quality profile");

    repo.createRule(CustomMessageSensor.RULE_KEY).setName("Issue With Custom Message")
      .setHtmlDescription("Generate an issue on each file with a custom message");

    repo.createRule(RandomAccessSensor.RULE_KEY).setName("One Issue Per File with Random Access")
      .setHtmlDescription("This issue is generated on each file");

    repo.createRule(MultilineIssuesSensor.RULE_KEY).setName("Creates issues with ranges/multiple locations")
      .setHtmlDescription("Issue with range and multiple locations");

    repo.createRule(OneIssuePerUnknownFileSensor.RULE_KEY).setName("Creates issues on each file with extension 'unknown'")
      .setHtmlDescription("This issue is generated on each file with extenstion 'unknown'");

    NewRule oneBugIssuePerLine = repo.createRule(OneBugIssuePerLineSensor.RULE_KEY).setName("One Bug Issue Per Line")
      .setHtmlDescription("Generate a bug issue on each line of a file. It requires the metric \"lines\".")
      .setType(RuleType.BUG);
    oneBugIssuePerLine
      .setDebtRemediationFunction(oneBugIssuePerLine.debtRemediationFunctions().linear("5min"));

    NewRule oneVulnerabilityIssuePerModule = repo.createRule(OneVulnerabilityIssuePerModuleSensor.RULE_KEY).setName("One Vulnerability Issue Per Module")
      .setHtmlDescription("Generate an issue on each module")
      .setType(RuleType.VULNERABILITY);
    oneVulnerabilityIssuePerModule
      .setDebtRemediationFunction(oneVulnerabilityIssuePerModule.debtRemediationFunctions().linearWithOffset("25min", "1h"))
      .setGapDescription("A certified architect will need roughly half an hour to start working on removal of modules, " +
        "then it's about one hour per module.");

    repo
      .createRule("xoo-template")
      .setTemplate(true)
      .setName("Template of rule")
      .setHtmlDescription("Template to be overridden by custom rules");

    NewRule hotspot = repo.createRule(HotspotSensor.RULE_KEY)
      .setName("Find security hotspots")
      .setType(RuleType.SECURITY_HOTSPOT)
      .setActivatedByDefault(false)
      .setHtmlDescription("Search for Security Hotspots in Xoo files");
    hotspot
      .setDebtRemediationFunction(hotspot.debtRemediationFunctions().constantPerIssue("2min"));

    if (version != null && version.isGreaterThanOrEqual(Version.create(7, 3))) {
      hotspot
        .addOwaspTop10(OwaspTop10.A1, OwaspTop10.A3)
        .addCwe(1, 123, 863);
    }

    repo.done();
  }

  private static void defineRulesXooExternal(Context context) {
    NewRepository repo = context.createExternalRepository(OneExternalIssuePerLineSensor.ENGINE_ID, Xoo.KEY).setName(OneExternalIssuePerLineSensor.ENGINE_ID);

    repo.createRule(OnePredefinedRuleExternalIssuePerLineSensor.RULE_ID)
      .setSeverity(OnePredefinedRuleExternalIssuePerLineSensor.SEVERITY)
      .setType(OnePredefinedRuleExternalIssuePerLineSensor.TYPE)
      .setScope(RuleScope.ALL)
      .setHtmlDescription("Generates one external issue in each line")
      .setName("One external issue per line");

    repo.done();
  }

}
