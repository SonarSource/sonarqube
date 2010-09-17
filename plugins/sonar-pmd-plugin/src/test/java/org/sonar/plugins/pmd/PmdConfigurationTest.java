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
package org.sonar.plugins.pmd;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.test.MavenTestUtils;

public class PmdConfigurationTest {

  @Test
  public void writeConfigurationToWorkingDir() throws IOException {
    Project project = MavenTestUtils.loadProjectFromPom(getClass(), "writeConfigurationToWorkingDir/pom.xml");

    PmdConfiguration configuration = new PmdConfiguration(new PmdProfileExporter(), RulesProfile.create(), project);
    List<String> rulesets = configuration.getRulesets();

    assertThat(rulesets.size(), is(1));
    File xmlFile = new File(rulesets.get(0));
    assertThat(xmlFile.exists(), is(true));
    assertThat(FileUtils.readFileToString(xmlFile), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<ruleset />\r\n\r\n"));
  }

  @Test
  public void findConfigurationToReuse() throws IOException {
    Project project = MavenTestUtils.loadProjectFromPom(getClass(), "findConfigurationToReuse/pom.xml");

    PmdConfiguration configuration = new PmdConfiguration(null, null, project);
    List<String> rulesets = configuration.getRulesets();
    assertThat(rulesets.size(), is(2));
    assertThat(rulesets, hasItem("ruleset/maven.xml"));
    assertThat(rulesets, hasItem("ruleset/basic.xml"));
  }

  @Test(expected = RuntimeException.class)
  public void failIfConfigurationToReuseDoesNotExist() throws IOException {
    Project project = MavenTestUtils.loadProjectFromPom(getClass(), "failIfConfigurationToReuseDoesNotExist/pom.xml");

    PmdConfiguration configuration = new PmdConfiguration(null, null, project);
    configuration.getRulesets();
  }

}
