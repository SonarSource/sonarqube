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

import org.apache.commons.configuration.Configuration;
import org.apache.maven.project.MavenProject;
import static org.hamcrest.CoreMatchers.is;
import org.hamcrest.core.Is;
import static org.hamcrest.text.StringEndsWith.endsWith;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.test.SimpleProjectFileSystem;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FindbugsMavenPluginHandlerTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  
  private Project project;
  private ProjectFileSystem fs;
  private File fakeSonarConfig;
  private MavenPlugin plugin;
  private FindbugsMavenPluginHandler handler;
  private File findbugsTempDir;

  @Before
  public void setup() {
    project = mock(Project.class);
    fs = mock(ProjectFileSystem.class);
    fakeSonarConfig = mock(File.class);
    plugin = mock(MavenPlugin.class);
    handler = createMavenPluginHandler();
    findbugsTempDir = tempFolder.newFolder("findbugs");
  }

  @Test
  public void doOverrideConfig() throws Exception {
    setupConfig();

    handler.configureFilters(project, plugin);
    verify(plugin).setParameter("includeFilterFile", "fakeSonarConfig.xml");
  }

  @Test
  public void doReuseExistingRulesConfig() throws Exception {
    setupConfig();
    // See sonar 583
    when(project.getReuseExistingRulesConfig()).thenReturn(true);
    when(plugin.getParameter("excludeFilterFile")).thenReturn("existingConfig.xml");

    handler.configureFilters(project, plugin);
    verify(plugin, never()).setParameter(eq("includeFilterFile"), anyString());
    
    setupConfig();
    when(project.getReuseExistingRulesConfig()).thenReturn(true);
    when(plugin.getParameter("includeFilterFile")).thenReturn("existingConfig.xml");

    handler.configureFilters(project, plugin);
    verify(plugin, never()).setParameter(eq("includeFilterFile"), anyString());
  }

  private void setupConfig() throws IOException {
    when(fakeSonarConfig.getCanonicalPath()).thenReturn("fakeSonarConfig.xml");
    when(project.getFileSystem()).thenReturn(fs);
    when(fs.writeToWorkingDirectory(anyString(), anyString())).thenReturn(fakeSonarConfig);
  }

  @Test
  public void shoulConfigurePlugin() throws URISyntaxException, IOException {

    mockProject(CoreProperties.FINDBUGS_EFFORT_DEFAULT_VALUE);

    handler.configure(project, plugin);

    verify(plugin).setParameter("skip", "false");
    verify(plugin).setParameter("xmlOutput", "true");
    verify(plugin).setParameter("threshold", "Low");
    verify(plugin).setParameter("effort", CoreProperties.FINDBUGS_EFFORT_DEFAULT_VALUE, false);
    verify(plugin).setParameter(eq("classFilesDirectory"), anyString());
    verify(plugin).setParameter(eq("includeFilterFile"), argThat(endsWith("findbugs-include.xml")));
    assertFindbugsIncludeFileIsSaved();
  }

  @Test(expected = SonarException.class)
  public void shoulFailIfNoCompiledClasses() throws URISyntaxException, IOException {
    when(project.getFileSystem()).thenReturn(fs);

    handler.configure(project, plugin);
  }

  @Test
  public void shouldConfigureEffort() throws URISyntaxException, IOException {
    FindbugsMavenPluginHandler handler = createMavenPluginHandler();
    mockProject("EffortSetInPom");
    MavenPlugin plugin = mock(MavenPlugin.class);

    handler.configure(project, plugin);

    verify(plugin).setParameter("effort", "EffortSetInPom", false);
  }

  @Test
  public void shouldConvertAntToJavaRegexp() {
    // see SONAR-853
    assertAntPatternEqualsToFindBugsRegExp("?", "~.", "g");
    assertAntPatternEqualsToFindBugsRegExp("*/myClass.JaVa", "~([^\\\\^\\s]*\\.)?myClass", "foo.bar.test.myClass");
    assertAntPatternEqualsToFindBugsRegExp("*/myClass.java", "~([^\\\\^\\s]*\\.)?myClass", "foo.bar.test.myClass");
    assertAntPatternEqualsToFindBugsRegExp("*/myClass2.jav", "~([^\\\\^\\s]*\\.)?myClass2", "foo.bar.test.myClass2");
    assertAntPatternEqualsToFindBugsRegExp("*/myOtherClass", "~([^\\\\^\\s]*\\.)?myOtherClass", "foo.bar.test.myOtherClass");
    assertAntPatternEqualsToFindBugsRegExp("*", "~[^\\\\^\\s]*", "ga.%#123_(*");
    assertAntPatternEqualsToFindBugsRegExp("**", "~.*", "gd.3reqg.3151];9#@!");
    assertAntPatternEqualsToFindBugsRegExp("**/generated/**", "~(.*\\.)?generated\\..*", "!@$Rq/32T$).generated.##TR.e#@!$");
    assertAntPatternEqualsToFindBugsRegExp("**/cl*nt/*", "~(.*\\.)?cl[^\\\\^\\s]*nt\\.[^\\\\^\\s]*", "!#$_.clr31r#!$(nt.!#$QRW)(.");
    assertAntPatternEqualsToFindBugsRegExp("**/org/apache/commons/**", "~(.*\\.)?org\\.apache\\.commons\\..*", "org.apache.commons.httpclient.contrib.ssl");
    assertAntPatternEqualsToFindBugsRegExp("*/org/apache/commons/**", "~([^\\\\^\\s]*\\.)?org\\.apache\\.commons\\..*", "org.apache.commons.httpclient.contrib.ssl");
    assertAntPatternEqualsToFindBugsRegExp("org/apache/commons/**", "~org\\.apache\\.commons\\..*", "org.apache.commons.httpclient.contrib.ssl");
  }

  @Test
  public void shouldntMatchThoseClassPattern() {
    // see SONAR-853
    assertJavaRegexpResult("[^\\\\^\\s]", "fad f.ate 12#)", false);
  }

  private void assertAntPatternEqualsToFindBugsRegExp(String antPattern, String regExp, String example) {
    assertThat(FindbugsAntConverter.antToJavaRegexpConvertor(antPattern), Is.is(regExp));
    String javaRegexp = regExp.substring(1, regExp.length());
    assertJavaRegexpResult(javaRegexp, example, true);
  }

  private void assertJavaRegexpResult(String javaRegexp, String example, boolean expectedResult) {
    Pattern pattern = Pattern.compile(javaRegexp);
    Matcher matcher = pattern.matcher(example);
    assertThat(example + " tested with pattern " + javaRegexp, matcher.matches(), Is.is(expectedResult));
  }

  private void assertFindbugsIncludeFileIsSaved() {
    File findbugsIncludeFile = new File(findbugsTempDir + "/target/sonar/findbugs-include.xml");
    assertThat(findbugsIncludeFile.exists(), is(true));
  }

  private FindbugsMavenPluginHandler createMavenPluginHandler() {
    return new FindbugsMavenPluginHandler(RulesProfile.create(), new FindbugsProfileExporter());
  }

  private void mockProject(String effort) throws URISyntaxException, IOException {
    when(project.getPom()).thenReturn(new MavenProject());
    when(project.getFileSystem()).thenReturn(new SimpleProjectFileSystem(findbugsTempDir));

    Configuration conf = mock(Configuration.class);
    when(project.getConfiguration()).thenReturn(conf);
    when(conf.getString(eq(CoreProperties.FINDBUGS_EFFORT_PROPERTY), anyString())).thenReturn(effort);

  }
}
