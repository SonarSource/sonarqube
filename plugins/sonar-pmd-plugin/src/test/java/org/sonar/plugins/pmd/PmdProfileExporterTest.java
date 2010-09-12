package org.sonar.plugins.pmd;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.test.TestUtils;
import org.xml.sax.SAXException;

public class PmdProfileExporterTest {

  private PmdProfileExporter exporter = new PmdProfileExporter();

  @Test
  public void testExportProfile() throws IOException, SAXException {
    List<ActiveRule> activeRulesExpected = buildActiveRulesFixture(buildRulesFixture());
    RulesProfile activeProfile = new RulesProfile();
    activeProfile.setActiveRules(activeRulesExpected);
    activeProfile.setName("A test profile");
    StringWriter xmlOutput = new StringWriter();
    exporter.exportProfile(activeProfile, xmlOutput);
    assertXmlAreSimilar(xmlOutput.toString(), "test_xml_complete.xml");
  }

  private List<org.sonar.api.rules.Rule> buildRulesFixture() {
    final Rule rule1 = new Rule("Coupling Between Objects", "CouplingBetweenObjects",
        "rulesets/coupling.xml/CouplingBetweenObjects", null, CoreProperties.PMD_PLUGIN, null);
    RuleParam ruleParam1 = new RuleParam(rule1, "threshold", null, "i");
    rule1.setParams(Arrays.asList(ruleParam1));

    final Rule rule2 = new Rule("Excessive Imports", "ExcessiveImports",
        "rulesets/coupling.xml/ExcessiveImports", null, CoreProperties.PMD_PLUGIN, null);
    RuleParam ruleParam2 = new RuleParam(rule2, "max", null, "i");
    rule2.setParams(Arrays.asList(ruleParam2));

    final Rule rule3 = new Rule("Use Notify All Instead Of Notify", "UseNotifyAllInsteadOfNotify",
        "rulesets/design.xml/UseNotifyAllInsteadOfNotify", null, CoreProperties.PMD_PLUGIN, null);

    final org.sonar.api.rules.Rule rule4 = new org.sonar.api.rules.Rule("Class names should always begin with an upper case character.",
        "ClassNamingConventions",
        "rulesets/naming.xml/ClassNamingConventions", null, CoreProperties.PMD_PLUGIN, null);

    return Arrays.asList(rule1, rule2, rule3, rule4);
  }

  private List<ActiveRule> buildActiveRulesFixture(List<org.sonar.api.rules.Rule> rules) {
    List<ActiveRule> activeRules = new ArrayList<ActiveRule>();

    ActiveRule activeRule1 = new ActiveRule(null, rules.get(0), RulePriority.CRITICAL);
    activeRule1.setActiveRuleParams(Arrays.asList(new ActiveRuleParam(activeRule1, rules.get(0).getParams().get(0), "20")));
    activeRules.add(activeRule1);

    ActiveRule activeRule2 = new ActiveRule(null, rules.get(1), RulePriority.MAJOR);
    activeRule2.setActiveRuleParams(Arrays.asList(new ActiveRuleParam(activeRule2, rules.get(1).getParams().get(0), "30")));
    activeRules.add(activeRule2);

    ActiveRule activeRule3 = new ActiveRule(null, rules.get(2), RulePriority.MINOR);
    activeRules.add(activeRule3);

    return activeRules;
  }

  private void assertXmlAreSimilar(String xml, String xmlFileToFind) throws IOException, SAXException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/pmd/" + xmlFileToFind);
    String xmlToFind = IOUtils.toString(input);
    TestUtils.assertSimilarXml(xmlToFind, xml);
  }

}
