/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.findbugs;

import org.apache.commons.io.IOUtils;
import org.hamcrest.BaseMatcher;
import static org.hamcrest.CoreMatchers.is;
import org.hamcrest.Description;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.sonar.api.CoreProperties;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FindbugsRulesRepositoryTest extends FindbugsTests {

  private FindbugsRulesRepository repository;

  @Before
  public void setup() {
    repository = new FindbugsRulesRepository(new Java());
  }

  @Test
  public void rulesAreDefinedWithTheDefaultSonarXmlFormat() {
    List<Rule> rules = repository.getInitialReferential();
    assertTrue(rules.size() > 0);
    for (Rule rule : rules) {
      assertNotNull(rule.getKey());
      assertNotNull(rule.getDescription());
      assertNotNull(rule.getConfigKey());
      assertNotNull(rule.getName());
    }
  }

  @Test
  @Ignore
  public void shouldProvideProfiles() {
    List<RulesProfile> profiles = repository.getProvidedProfiles();
    assertThat(profiles.size(), is(1));

    RulesProfile profile1 = profiles.get(0);
    assertThat(profile1.getName(), is(RulesProfile.SONAR_WAY_FINDBUGS_NAME));
    assertEquals(profile1.getActiveRules().size(), 344);
  }

  @Test
  public void shouldAddHeaderToExportedXml() throws IOException, SAXException {
    RulesProfile rulesProfile = mock(RulesProfile.class);
    when(rulesProfile.getActiveRulesByPlugin(CoreProperties.FINDBUGS_PLUGIN)).thenReturn(Collections.<ActiveRule>emptyList());

    assertXmlAreSimilar(repository.exportConfiguration(rulesProfile), "test_header.xml");
  }

  @Test
  public void shouldExportConfiguration() throws IOException, SAXException {
    List<Rule> rules = buildRulesFixture();
    List<ActiveRule> activeRulesExpected = buildActiveRulesFixture(rules);
    RulesProfile activeProfile = new RulesProfile();
    activeProfile.setActiveRules(activeRulesExpected);

    assertXmlAreSimilar(repository.exportConfiguration(activeProfile), "test_xml_complete.xml");
  }

  @Test
  public void shouldImportPatterns() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/FindbugsRulesRepositoryTest/shouldImportPatterns.xml");
    List<ActiveRule> results = repository.importConfiguration(IOUtils.toString(input), buildRulesFixtureImport());

    assertThat(results.size(), is(2));
    assertThat(results, new ContainsActiveRule("FB1_IMPORT_TEST_1", RulePriority.MAJOR));
    assertThat(results, new ContainsActiveRule("FB2_IMPORT_TEST_4", RulePriority.MAJOR));
  }

  @Test
  public void shouldImportCodes() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/FindbugsRulesRepositoryTest/shouldImportCodes.xml");
    List<ActiveRule> results = repository.importConfiguration(IOUtils.toString(input), buildRulesFixtureImport());

    assertThat(results.size(), is(4));
    assertThat(results, new ContainsActiveRule("FB1_IMPORT_TEST_1", RulePriority.MAJOR));
    assertThat(results, new ContainsActiveRule("FB1_IMPORT_TEST_2", RulePriority.MAJOR));
    assertThat(results, new ContainsActiveRule("FB1_IMPORT_TEST_3", RulePriority.MAJOR));
    assertThat(results, new ContainsActiveRule("FB3_IMPORT_TEST_5", RulePriority.MAJOR));
  }

  @Test
  public void shouldImportCategories() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/FindbugsRulesRepositoryTest/shouldImportCategories.xml");
    List<ActiveRule> results = repository.importConfiguration(IOUtils.toString(input), buildRulesFixtureImport());

    assertThat(results.size(), is(4));
    assertThat(results, new ContainsActiveRule("FB1_IMPORT_TEST_1", RulePriority.INFO));
    assertThat(results, new ContainsActiveRule("FB1_IMPORT_TEST_2", RulePriority.INFO));
    assertThat(results, new ContainsActiveRule("FB1_IMPORT_TEST_3", RulePriority.INFO));
    assertThat(results, new ContainsActiveRule("FB2_IMPORT_TEST_4", RulePriority.INFO));
  }

  @Test
  public void shouldImportConfigurationBugInclude() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/findbugs-include.xml");
    List<ActiveRule> results = repository.importConfiguration(IOUtils.toString(input), buildRulesFixtureImport());

    assertThat(results.size(), is(4));
    assertThat(results, new ContainsActiveRule("FB1_IMPORT_TEST_1", null));
    assertThat(results, new ContainsActiveRule("FB1_IMPORT_TEST_2", null));
    assertThat(results, new ContainsActiveRule("FB1_IMPORT_TEST_3", null));
    assertThat(results, new ContainsActiveRule("FB2_IMPORT_TEST_4", null));
  }

  private static List<Rule> buildRulesFixtureImport() {
    Rule rule1 = new Rule("Correctness - Import test 1 group 1", "FB1_IMPORT_TEST_1",
        "FB1_IMPORT_TEST_1", null, CoreProperties.FINDBUGS_PLUGIN, null);

    Rule rule2 = new Rule("Multithreaded correctness - Import test 2 group 1", "FB1_IMPORT_TEST_2",
        "FB1_IMPORT_TEST_2", null, CoreProperties.FINDBUGS_PLUGIN, null);

    Rule rule3 = new Rule("Multithreaded correctness - Import test 3 group 1", "FB1_IMPORT_TEST_3",
        "FB1_IMPORT_TEST_3", null, CoreProperties.FINDBUGS_PLUGIN, null);

    Rule rule4 = new Rule("Multithreaded correctness - Import test 4 group 2", "FB2_IMPORT_TEST_4",
        "FB2_IMPORT_TEST_4", null, CoreProperties.FINDBUGS_PLUGIN, null);

    Rule rule5 = new Rule("Style - Import test 5 group 3", "FB3_IMPORT_TEST_5",
        "FB3_IMPORT_TEST_5", null, CoreProperties.FINDBUGS_PLUGIN, null);

    return Arrays.asList(rule1, rule2, rule3, rule4, rule5);
  }
}

class ContainsActiveRule extends BaseMatcher<List<ActiveRule>> {
  private String key;
  private RulePriority priority;

  ContainsActiveRule(String key, RulePriority priority) {
    this.key = key;
    this.priority = priority;
  }

  public boolean matches(Object o) {
    List<ActiveRule> rules = (List<ActiveRule>) o;
    for (ActiveRule rule : rules) {
      if (rule.getRule().getKey().equals(key)) {
        if (priority == null) {
          return true;
        }
        return rule.getPriority().equals(priority);
      }
    }
    return false;
  }

  public void describeTo(Description description) {
  }
}
