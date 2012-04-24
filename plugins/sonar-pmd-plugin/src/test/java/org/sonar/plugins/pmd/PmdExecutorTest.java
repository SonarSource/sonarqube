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
package org.sonar.plugins.pmd;

import com.google.common.base.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PmdExecutorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void executeOnManySourceDirs() throws IOException {
    File workDir = temp.getRoot();
    Project project = new Project("two-source-dirs");

    ProjectFileSystem fs = mock(ProjectFileSystem.class);
    File root = new File(getClass().getResource("/org/sonar/plugins/pmd/PmdExecutorTest/executeOnManySourceDirs/").toURI());
    when(fs.getSourceFiles(Java.INSTANCE)).thenReturn(Arrays.asList(new File(root, "src1/FirstClass.java"), new File(root, "src2/SecondClass.java")));
    when(fs.getSourceCharset()).thenReturn(Charsets.UTF_8);
    when(fs.getSonarWorkingDirectory()).thenReturn(workDir);
    project.setFileSystem(fs);

    PmdConfiguration conf = mock(PmdConfiguration.class);
    File file = FileUtils.toFile(getClass().getResource("/org/sonar/plugins/pmd/PmdExecutorTest/executeOnManySourceDirs/pmd.xml").toURI().toURL());
    when(conf.getRulesets()).thenReturn(Arrays.asList(file.getAbsolutePath()));
    File xmlReport = new File(workDir, "pmd-result.xml");
    when(conf.getTargetXMLReport()).thenReturn(xmlReport);

    PmdExecutor executor = new PmdExecutor(project, conf);
    executor.execute();
    assertThat(xmlReport.exists(), is(true));

    String xml = FileUtils.readFileToString(xmlReport);

    // errors on the two source files
    assertThat(StringUtils.countMatches(xml, "<file"), is(2));
    assertThat(StringUtils.countMatches(xml, "<violation"), greaterThan(2));
  }

  @Test
  public void ignorePmdFailures() throws IOException {
    final File workDir = temp.getRoot();
    Project project = new Project("ignorePmdFailures");

    ProjectFileSystem fs = mock(ProjectFileSystem.class);
    when(fs.getSourceFiles(Java.INSTANCE)).thenReturn(Arrays.asList(new File("test-resources/ignorePmdFailures/DoesNotCompile.java")));
    when(fs.getSourceCharset()).thenReturn(Charsets.UTF_8);
    when(fs.getSonarWorkingDirectory()).thenReturn(workDir);
    project.setFileSystem(fs);

    PmdConfiguration conf = mock(PmdConfiguration.class);
    when(conf.getRulesets()).thenReturn(Arrays.asList(new File("test-resources/ignorePmdFailures/pmd.xml").getAbsolutePath()));
    File xmlReport = new File(workDir, "pmd-result.xml");
    when(conf.getTargetXMLReport()).thenReturn(xmlReport);
    PmdExecutor executor = new PmdExecutor(project, conf);

    executor.execute();
    assertThat(xmlReport.exists(), is(true));
  }

  @Test
  public void shouldNormalizeJavaVersion() {
    assertThat(PmdExecutor.getNormalizedJavaVersion(null), nullValue());
    assertThat(PmdExecutor.getNormalizedJavaVersion(""), is(""));
    assertThat(PmdExecutor.getNormalizedJavaVersion("1.1"), is("1.3"));
    assertThat(PmdExecutor.getNormalizedJavaVersion("1.2"), is("1.3"));
    assertThat(PmdExecutor.getNormalizedJavaVersion("1.4"), is("1.4"));
    assertThat(PmdExecutor.getNormalizedJavaVersion("5"), is("1.5"));
    assertThat(PmdExecutor.getNormalizedJavaVersion("6"), is("1.6"));
  }

}
