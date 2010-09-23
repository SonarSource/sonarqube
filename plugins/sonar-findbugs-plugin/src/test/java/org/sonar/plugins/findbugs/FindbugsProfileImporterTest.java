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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plugins.findbugs.xml.FindBugsFilter;
import org.sonar.plugins.findbugs.xml.Match;
import org.sonar.test.TestUtils;

import com.thoughtworks.xstream.XStream;

public class FindbugsProfileImporterTest {

  private FindbugsProfileImporter importer = new FindbugsProfileImporter(new FindbugsRuleFinder());

  @Test
  public void shouldImportPatterns() throws IOException {
    String findbugsConf = TestUtils.getResourceContent("/org/sonar/plugins/findbugs/shouldImportPatterns.xml");
    RulesProfile profile = importer.importProfile(new StringReader(findbugsConf), ValidationMessages.create());

    assertThat(profile.getActiveRules().size(), is(2));
    assertThat(profile.getActiveRule(FindbugsConstants.REPOSITORY_KEY, "NP_CLOSING_NULL"), is(notNullValue()));
    assertThat(profile.getActiveRule(FindbugsConstants.REPOSITORY_KEY, "RC_REF_COMPARISON_BAD_PRACTICE"), is(notNullValue()));
  }

  @Test
  public void shouldImportCodes() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/shouldImportCodes.xml");
    RulesProfile profile = importer.importProfile(new InputStreamReader(input), ValidationMessages.create());
    List<ActiveRule> results = profile.getActiveRules();

    assertThat(results.size(), is(18));
    assertThat(profile.getActiveRule(FindbugsConstants.REPOSITORY_KEY, "EC_INCOMPATIBLE_ARRAY_COMPARE"), is(notNullValue()));
    assertThat(profile.getActiveRule(FindbugsConstants.REPOSITORY_KEY, "BC_IMPOSSIBLE_DOWNCAST_OF_TOARRAY"), is(notNullValue()));
  }

  @Test
  public void shouldImportCategories() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/shouldImportCategories.xml");
    RulesProfile profile = importer.importProfile(new InputStreamReader(input), ValidationMessages.create());
    List<ActiveRule> results = profile.getActiveRules();

    assertThat(results.size(), is(183));
    assertThat(profile.getActiveRule(FindbugsConstants.REPOSITORY_KEY, "LG_LOST_LOGGER_DUE_TO_WEAK_REFERENCE"), is(notNullValue()));
  }

  @Test
  public void shouldImportConfigurationBugInclude() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/findbugs-include.xml");
    RulesProfile profile = importer.importProfile(new InputStreamReader(input), ValidationMessages.create());
    List<ActiveRule> results = profile.getActiveRules();

    assertThat(results.size(), is(11));
    assertThat(profile.getActiveRule(FindbugsConstants.REPOSITORY_KEY, "RC_REF_COMPARISON_BAD_PRACTICE"), is(notNullValue()));
  }

  @Test
  public void shouldBuilModuleTreeFromXml() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/findbugs/test_module_tree.xml");

    XStream xStream = FindBugsFilter.createXStream();
    FindBugsFilter filter = (FindBugsFilter) xStream.fromXML(IOUtils.toString(input));

    List<Match> matches = filter.getMatchs();
    assertThat(matches.size(), is(2));
    assertThat(matches.get(0).getBug().getPattern(), is("DLS_DEAD_LOCAL_STORE"));
    assertThat(matches.get(1).getBug().getPattern(), is("URF_UNREAD_FIELD"));
  }

  @Test
  public void testImportingUncorrectXmlFile() throws IOException {
    String uncorrectFindbugsXml = TestUtils.getResourceContent("/org/sonar/plugins/findbugs/uncorrectFindbugsXml.xml");
    ValidationMessages messages = ValidationMessages.create();
    RulesProfile profile = importer.importProfile(new StringReader(uncorrectFindbugsXml), messages);
    List<ActiveRule> results = profile.getActiveRules();

    assertThat(results.size(), is(0));
    assertThat(messages.getErrors().size(), is(1));
  }

  @Test
  public void testImportingXmlFileWithUnknownRule() throws IOException {
    String uncorrectFindbugsXml = TestUtils.getResourceContent("/org/sonar/plugins/findbugs/findbugsXmlWithUnknownRule.xml");
    ValidationMessages messages = ValidationMessages.create();
    RulesProfile profile = importer.importProfile(new StringReader(uncorrectFindbugsXml), messages);
    List<ActiveRule> results = profile.getActiveRules();

    assertThat(results.size(), is(1));
    assertThat(messages.getWarnings().size(), is(1));
  }

  @Test
  public void testImportingXmlFileWithUnknownCategory() throws IOException {
    String uncorrectFindbugsXml = TestUtils.getResourceContent("/org/sonar/plugins/findbugs/findbugsXmlWithUnknownCategory.xml");
    ValidationMessages messages = ValidationMessages.create();
    RulesProfile profile = importer.importProfile(new StringReader(uncorrectFindbugsXml), messages);
    List<ActiveRule> results = profile.getActiveRules();

    assertThat(results.size(), is(142));
    assertThat(messages.getWarnings().size(), is(1));
  }

  @Test
  public void testImportingXmlFileWithUnknownCode() throws IOException {
    String uncorrectFindbugsXml = TestUtils.getResourceContent("/org/sonar/plugins/findbugs/findbugsXmlWithUnknownCode.xml");
    ValidationMessages messages = ValidationMessages.create();
    RulesProfile profile = importer.importProfile(new StringReader(uncorrectFindbugsXml), messages);
    List<ActiveRule> results = profile.getActiveRules();

    assertThat(results.size(), is(9));
    assertThat(messages.getWarnings().size(), is(1));
  }
}
