/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.checkstyle;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.test.MavenTestUtils;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CheckstyleConfigurationTest {

  @Test
  public void writeConfigurationToWorkingDir() throws IOException {
    Project project = MavenTestUtils.loadProjectFromPom(getClass(), "writeConfigurationToWorkingDir/pom.xml");

    CheckstyleProfileExporter exporter = new FakeExporter();
    CheckstyleConfiguration configuration = new CheckstyleConfiguration(null, exporter, null, project.getFileSystem());
    File xmlFile = configuration.getXMLDefinitionFile();

    assertThat(xmlFile.exists(), is(true));
    assertThat(FileUtils.readFileToString(xmlFile), is("<conf/>"));
  }

  public class FakeExporter extends CheckstyleProfileExporter {
    @Override
    public void exportProfile(RulesProfile profile, Writer writer) {
      try {
        writer.write("<conf/>");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
