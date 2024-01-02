/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.api.server.rule.RuleDescriptionSection;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionAnnotationLoader;
import org.sonar.api.utils.Version;
import org.sonar.xoo.Xoo;
import org.sonar.xoo.Xoo2;
import org.sonar.xoo.checks.Check;
import org.sonar.xoo.rule.hotspot.HotspotWithContextsSensor;
import org.sonar.xoo.rule.hotspot.HotspotWithSingleContextSensor;
import org.sonar.xoo.rule.hotspot.HotspotWithoutContextSensor;

import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ASSESS_THE_PROBLEM_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.HOW_TO_FIX_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.INTRODUCTION_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.RESOURCES_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ROOT_CAUSE_SECTION_KEY;
import static org.sonar.api.server.rule.RulesDefinition.OwaspTop10Version.Y2017;
import static org.sonar.api.server.rule.RulesDefinition.OwaspTop10Version.Y2021;

/**
 * Define all the coding rules that are supported on the repositories named "xoo" and "xoo2"
 */
public class XooRulesDefinition implements RulesDefinition {

  public static final String[] AVAILABLE_CONTEXTS = {"javascript", "jquery", "express_js", "react", "axios"};

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

    NewRule hasTag = repo.createRule(HasTagSensor.RULE_KEY).setName("Has Tag");
    addAllDescriptionSections(hasTag, "Search for a given tag in Xoo files");

    NewRule oneIssuePerLine = repo.createRule(OneIssuePerLineSensor.RULE_KEY).setName("One Issue Per Line");
    addAllDescriptionSections(oneIssuePerLine, "Generate an issue on each line of a file. It requires the metric \"lines\".");
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
      .addDescriptionSection(howToFixSectionWithContext("single_context"));
    addDescriptionSectionsWithoutContexts(hasTag, "Search for a given tag in Xoo files");

    hasTag
      .setDebtRemediationFunction(hasTag.debtRemediationFunctions().constantPerIssue("2min"));
    hasTag.createParam("tag")
      .setDefaultValue("xoo")
      .setDescription("The tag to search for");

    NewRule ruleWithParameters = repo.createRule("RuleWithParameters").setName("Rule with parameters");
    addAllDescriptionSections(ruleWithParameters,
      "Rule containing parameter of different types : boolean, integer, etc. For information, no issue will be linked to this rule.");
    ruleWithParameters.createParam("string").setType(RuleParamType.STRING);
    ruleWithParameters.createParam("text").setType(RuleParamType.TEXT);
    ruleWithParameters.createParam("boolean").setType(RuleParamType.BOOLEAN);
    ruleWithParameters.createParam("integer").setType(RuleParamType.INTEGER);
    ruleWithParameters.createParam("float").setType(RuleParamType.FLOAT);

    NewRule oneIssuePerLine = repo.createRule(OneIssuePerLineSensor.RULE_KEY).setName("One Issue Per Line")
      .setTags("line");
    addDescriptionSectionsWithoutContexts(oneIssuePerLine, "Generate an issue on each line of a file. It requires the metric \"lines\".");
    addHowToFixSectionsWithContexts(oneIssuePerLine);
    oneIssuePerLine
      .setDebtRemediationFunction(oneIssuePerLine.debtRemediationFunctions().linear("1min"))
      .setGapDescription("It takes about 1 minute to an experienced software craftsman to remove a line of code")
      .addEducationPrincipleKeys("defense_in_depth", "never_trust_user_input");

    NewRule oneQuickFixPerLine = repo.createRule(OneQuickFixPerLineSensor.RULE_KEY).setName("One Quick Fix Per Line")
      .setTags("line");
    addAllDescriptionSections(oneQuickFixPerLine,
      "Generate an issue with quick fix available on each line of a file. It requires the metric \"lines\".");

    oneQuickFixPerLine
      .setDebtRemediationFunction(oneQuickFixPerLine.debtRemediationFunctions().linear("1min"))
      .setGapDescription("It takes about 1 minute to an experienced software craftsman to remove a line of code");

    NewRule oneIssueOnDirPerFile = repo.createRule(OneIssueOnDirPerFileSensor.RULE_KEY).setName("One Issue On Dir Per File");
    addAllDescriptionSections(oneIssueOnDirPerFile,
      "Generate issues on directories");
    NewRule oneIssuePerFile = repo.createRule(OneIssuePerFileSensor.RULE_KEY).setName("One Issue Per File");
    oneIssuePerFile.setDebtRemediationFunction(oneIssuePerFile.debtRemediationFunctions().linear(TEN_MIN));
    addAllDescriptionSections(oneIssuePerFile, "Generate an issue on each file");

    NewRule oneIssuePerTestFile = repo.createRule(OneIssuePerTestFileSensor.RULE_KEY).setName("One Issue Per Test File")
      .setScope(RuleScope.TEST);
    oneIssuePerTestFile.setDebtRemediationFunction(oneIssuePerTestFile.debtRemediationFunctions().linear("8min"));
    addAllDescriptionSections(oneIssuePerTestFile, "Generate an issue on each test file");

    NewRule oneBugIssuePerTestLine = repo.createRule(OneBugIssuePerTestLineSensor.RULE_KEY).setName("One Bug Issue Per Test Line")
      .setScope(RuleScope.TEST)
      .setType(RuleType.BUG);
    addAllDescriptionSections(oneBugIssuePerTestLine, "Generate a bug issue on each line of a test file. It requires the metric \"lines\".");

    oneBugIssuePerTestLine
      .setDebtRemediationFunction(oneBugIssuePerTestLine.debtRemediationFunctions().linear("4min"));

    NewRule oneCodeSmellIssuePerTestLine = repo.createRule(OneCodeSmellIssuePerTestLineSensor.RULE_KEY).setName("One Code Smell Issue Per Test Line")
      .setScope(RuleScope.TEST)
      .setType(RuleType.CODE_SMELL);
    addAllDescriptionSections(oneCodeSmellIssuePerTestLine, "Generate a code smell issue on each line of a test file. It requires the metric \"lines\".");

    oneCodeSmellIssuePerTestLine
      .setDebtRemediationFunction(oneCodeSmellIssuePerTestLine.debtRemediationFunctions().linear("3min"));

    NewRule oneIssuePerDirectory = repo.createRule(OneIssuePerDirectorySensor.RULE_KEY).setName("One Issue Per Directory");
    oneIssuePerDirectory.setDebtRemediationFunction(oneIssuePerDirectory.debtRemediationFunctions().linear(TEN_MIN));
    addAllDescriptionSections(oneIssuePerDirectory, "Generate an issue on each non-empty directory");

    NewRule oneDayDebtPerFile = repo.createRule(OneDayDebtPerFileSensor.RULE_KEY).setName("One Day Debt Per File");
    oneDayDebtPerFile.setDebtRemediationFunction(oneDayDebtPerFile.debtRemediationFunctions().linear("1d"));
    addAllDescriptionSections(oneDayDebtPerFile, "Generate an issue on each file with a debt of one day");

    NewRule oneIssuePerModule = repo.createRule(OneIssuePerModuleSensor.RULE_KEY).setName("One Issue Per Module");
    oneIssuePerModule
      .setDebtRemediationFunction(oneIssuePerModule.debtRemediationFunctions().linearWithOffset("25min", "1h"))
      .setGapDescription("A certified architect will need roughly half an hour to start working on removal of modules, " +
        "then it's about one hour per module.");
    addAllDescriptionSections(oneIssuePerModule, "Generate an issue on each module");

    NewRule oneBlockerIssuePerFile = repo.createRule(OneBlockerIssuePerFileSensor.RULE_KEY).setName("One Blocker Issue Per File");
    addAllDescriptionSections(oneBlockerIssuePerFile, "Generate a blocker issue on each file, whatever the severity declared in the Quality profile");

    NewRule issueWithCustomMessage = repo.createRule(CustomMessageSensor.RULE_KEY).setName("Issue With Custom Message");
    addAllDescriptionSections(issueWithCustomMessage, "Generate an issue on each file with a custom message");

    NewRule oneIssuePerFileWithRandomAccess = repo.createRule(RandomAccessSensor.RULE_KEY).setName("One Issue Per File with Random Access");
    addAllDescriptionSections(oneIssuePerFileWithRandomAccess, "This issue is generated on each file");

    NewRule issueWithRangeAndMultipleLocations = repo.createRule(MultilineIssuesSensor.RULE_KEY).setName("Creates issues with ranges/multiple locations");
    addAllDescriptionSections(issueWithRangeAndMultipleLocations, "Issue with range and multiple locations");

    NewRule hotspotWithRangeAndMultipleLocations = repo.createRule(MultilineHotspotSensor.RULE_KEY)
      .setName("Creates hotspots with ranges/multiple locations")
      .setType(RuleType.SECURITY_HOTSPOT);
    addAllDescriptionSections(hotspotWithRangeAndMultipleLocations, "Hotspot with range and multiple locations");


    NewRule issueOnEachFileWithExtUnknown = repo.createRule(OneIssuePerUnknownFileSensor.RULE_KEY).setName("Creates issues on each file with extension 'unknown'");
    addAllDescriptionSections(issueOnEachFileWithExtUnknown, "This issue is generated on each file with extenstion 'unknown'");

    NewRule oneBugIssuePerLine = repo.createRule(OneBugIssuePerLineSensor.RULE_KEY).setName("One Bug Issue Per Line")
      .setType(RuleType.BUG);
    oneBugIssuePerLine
      .setDebtRemediationFunction(oneBugIssuePerLine.debtRemediationFunctions().linear("5min"));
    addAllDescriptionSections(oneBugIssuePerLine, "Generate a bug issue on each line of a file. It requires the metric \"lines\".");

    NewRule oneCodeSmellIssuePerLine = repo.createRule(OneCodeSmellIssuePerLineSensor.RULE_KEY).setName("One Code Smell Issue Per Line")
      .setType(RuleType.CODE_SMELL);
    oneCodeSmellIssuePerLine
      .setDebtRemediationFunction(oneCodeSmellIssuePerLine.debtRemediationFunctions().linear("9min"));
    addAllDescriptionSections(oneCodeSmellIssuePerLine, "Generate a code smell issue on each line of a file. It requires the metric \"lines\".");

    NewRule oneVulnerabilityIssuePerModule = repo.createRule(OneVulnerabilityIssuePerModuleSensor.RULE_KEY).setName("One Vulnerability Issue Per Module")
      .setType(RuleType.VULNERABILITY);
    addAllDescriptionSections(oneVulnerabilityIssuePerModule, "Generate an issue on each module");

    oneVulnerabilityIssuePerModule
      .setDebtRemediationFunction(oneVulnerabilityIssuePerModule.debtRemediationFunctions().linearWithOffset("25min", "1h"))
      .setGapDescription("A certified architect will need roughly half an hour to start working on removal of modules, " +
        "then it's about one hour per module.");

    NewRule templateofRule = repo
      .createRule("xoo-template")
      .setTemplate(true)
      .setName("Template of rule");
    addAllDescriptionSections(templateofRule, "Template to be overridden by custom rules");

    NewRule hotspot = repo.createRule(HotspotWithoutContextSensor.RULE_KEY)
      .setName("Find security hotspots")
      .setType(RuleType.SECURITY_HOTSPOT)
      .setActivatedByDefault(false);
    addAllDescriptionSections(hotspot, "Search for Security Hotspots in Xoo files");

    hotspot
      .setDebtRemediationFunction(hotspot.debtRemediationFunctions().constantPerIssue("2min"));

    if (version != null && version.isGreaterThanOrEqual(Version.create(9, 3))) {
      hotspot
        .addOwaspTop10(OwaspTop10.A1, OwaspTop10.A3)
        .addOwaspTop10(Y2021, OwaspTop10.A3, OwaspTop10.A2)
        .addCwe(1, 89, 123, 863);

      oneVulnerabilityIssuePerModule
        .addOwaspTop10(Y2017, OwaspTop10.A9, OwaspTop10.A10)
        .addOwaspTop10(Y2021, OwaspTop10.A6, OwaspTop10.A9)
        .addCwe(250, 564, 546, 943);
    }

    if (version != null && version.isGreaterThanOrEqual(Version.create(9, 5))) {
      hotspot
        .addPciDss(PciDssVersion.V4_0, "6.5.1", "4.1")
        .addPciDss(PciDssVersion.V3_2, "6.5.1", "4.2")
        .addPciDss(PciDssVersion.V4_0, "6.5a.1", "4.2c")
        .addPciDss(PciDssVersion.V3_2, "6.5a.1b", "4.2b");

      oneVulnerabilityIssuePerModule
        .addPciDss(PciDssVersion.V4_0, "10.1")
        .addPciDss(PciDssVersion.V3_2, "10.2")
        .addPciDss(PciDssVersion.V4_0, "10.1a.2b")
        .addPciDss(PciDssVersion.V3_2, "10.1a.2c");
    }

    if (version != null && version.isGreaterThanOrEqual(Version.create(9, 6))) {
      hotspot
        .addOwaspAsvs(OwaspAsvsVersion.V4_0, "2.8.7", "3.1.1", "4.2.2");
      oneVulnerabilityIssuePerModule
        .addOwaspAsvs(OwaspAsvsVersion.V4_0, "11.1.2", "14.5.1", "14.5.4");
    }

    NewRule hotspotWithContexts = repo.createRule(HotspotWithContextsSensor.RULE_KEY)
      .setName("Find security hotspots with contexts")
      .setType(RuleType.SECURITY_HOTSPOT)
      .setActivatedByDefault(false);
    addDescriptionSectionsWithoutContexts(hotspotWithContexts, "Search for Security Hotspots with contexts in Xoo files");
    addHowToFixSectionsWithContexts(hotspotWithContexts);

    NewRule hotspotWithSingleContext = repo.createRule(HotspotWithSingleContextSensor.RULE_KEY)
      .setName("Find security hotspots, how_to_fix with single context")
      .setType(RuleType.SECURITY_HOTSPOT)
      .setActivatedByDefault(false)
      .addDescriptionSection(howToFixSectionWithContext("single_context"));
    addDescriptionSectionsWithoutContexts(hotspotWithSingleContext, "Search for Security Hotspots with single context in Xoo files");

    repo.done();
  }

  private static void defineRulesXooExternal(Context context) {
    NewRepository repo = context.createExternalRepository(OneExternalIssuePerLineSensor.ENGINE_ID, Xoo.KEY).setName(OneExternalIssuePerLineSensor.ENGINE_ID);

    repo.createRule(OnePredefinedRuleExternalIssuePerLineSensor.RULE_ID)
      .setSeverity(OnePredefinedRuleExternalIssuePerLineSensor.SEVERITY)
      .setType(OnePredefinedRuleExternalIssuePerLineSensor.TYPE)
      .setScope(RuleScope.ALL)
      .setHtmlDescription("Generates one external issue in each line")
      .addDescriptionSection(descriptionSection(INTRODUCTION_SECTION_KEY, "Generates one external issue in each line"))
      .setName("One external issue per line")
      .setTags("riri", "fifi", "loulou");

    repo.done();
  }

  private static void addAllDescriptionSections(NewRule rule, String description) {
    addDescriptionSectionsWithoutContexts(rule, description);
    rule.addDescriptionSection(descriptionSection(HOW_TO_FIX_SECTION_KEY, "How to fix: " + description));
  }

  private static void addDescriptionSectionsWithoutContexts(NewRule rule, String description) {
    rule
      .setHtmlDescription(description)
      .addDescriptionSection(descriptionSection(INTRODUCTION_SECTION_KEY, "Introduction: " + description))
      .addDescriptionSection(descriptionSection(ROOT_CAUSE_SECTION_KEY, "Root cause: " + description))
      .addDescriptionSection(descriptionSection(ASSESS_THE_PROBLEM_SECTION_KEY, "Assess the problem: " + description))
      .addDescriptionSection(descriptionSection(RESOURCES_SECTION_KEY,
        "<a href=\"www.google.fr\"> Google </a><br><a href=\"https://stackoverflow.com/\"> StackOverflow</a>"))
      .addDescriptionSection(descriptionSection("fake_section_to_be_ignored",
        "fake_section_to_be_ignored"));
  }

  private static void addHowToFixSectionsWithContexts(NewRule rule) {
    for (String contextName : AVAILABLE_CONTEXTS) {
      rule.addDescriptionSection(howToFixSectionWithContext(contextName));
    }
  }

  private static RuleDescriptionSection descriptionSection(String sectionKey, String htmlDescription) {
    return RuleDescriptionSection.builder()
      .sectionKey(sectionKey)
      .htmlContent(htmlDescription)
      .build();
  }

  private static RuleDescriptionSection howToFixSectionWithContext(String contextName) {
    return RuleDescriptionSection.builder()
      .sectionKey(HOW_TO_FIX_SECTION_KEY)
      .htmlContent(String.format("This is 'How to fix?' description section for the <a href=\"https://stackoverflow.com/\"> %s</a>. " +
        "This text can be very long.", contextName))
      .context(new org.sonar.api.server.rule.Context(contextName, contextName))
      .build();
  }
}
