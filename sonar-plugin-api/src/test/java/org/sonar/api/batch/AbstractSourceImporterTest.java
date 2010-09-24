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
package org.sonar.api.batch;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.CharEncoding;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.exceptions.verification.junit.ArgumentsAreDifferent;
import org.sonar.api.CoreProperties;
import org.sonar.api.resources.DefaultProjectFileSystem;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;

public class AbstractSourceImporterTest {

  private String aClaess;
  private String explicacao;
  private FakeSourceImporter importer;

  @Before
  public void setup() throws UnsupportedEncodingException {
    aClaess = new String(new byte[] { 65, 67, 108, 97, -61, -88, 115, 115, 40, 41 }, CharEncoding.UTF_8);
    explicacao = new String(new byte[] { 101, 120, 112, 108, 105, 99, 97, -61, -89, -61, -93, 111, 40, 41 }, CharEncoding.UTF_8);
    importer = new FakeSourceImporter();
  }

  @Test
  public void shouldBeEnabledByDefault() {
    Project pom = mock(Project.class);
    when(pom.getConfiguration()).thenReturn(new PropertiesConfiguration());
    assertTrue(importer.isEnabled(pom));
  }

  @Test
  public void canBeDisabled() {
    Project pom = mock(Project.class);
    Configuration config = mock(Configuration.class);
    when(pom.getConfiguration()).thenReturn(config);
    when(config.getBoolean(CoreProperties.CORE_IMPORT_SOURCES_PROPERTY, CoreProperties.CORE_IMPORT_SOURCES_DEFAULT_VALUE)).thenReturn(
        Boolean.FALSE);

    assertFalse(importer.isEnabled(pom));
    assertFalse(importer.shouldExecuteOnProject(pom));
  }

  @Test
  public void doNotSaveSourceIfNullResource() throws IOException {
    AbstractSourceImporter nullImporter = new AbstractSourceImporter(Java.INSTANCE) {

      @Override
      protected Resource createResource(File file, List<File> sourceDirs, boolean unitTest) {
        return null;
      }
    };

    SensorContext context = mock(SensorContext.class);
    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    when(fileSystem.getSourceFiles(Java.INSTANCE)).thenReturn(Arrays.<File> asList(new File("Foo.java"), new File("Bar.java")));
    nullImporter.analyse(fileSystem, context);

    verify(context, never()).saveSource((Resource) anyObject(), anyString());
  }

  @Test
  public void shouldUseMacRomanCharsetForReadingSourceFiles() {
    Project project = mock(Project.class);
    SensorContext context = mock(SensorContext.class);

    String encoding = "MacRoman";
    String testFile = "MacRomanEncoding.java";
    fileEncodingTest(project, context, encoding, testFile);
  }

  @Test
  public void shouldUseCP1252CharsetForReadingSourceFiles() {
    Project project = mock(Project.class);
    SensorContext context = mock(SensorContext.class);

    String encoding = "CP1252";
    String testFile = "CP1252Encoding.java";
    fileEncodingTest(project, context, encoding, testFile);
  }

  @Test(expected = ArgumentsAreDifferent.class)
  public void shouldFailWithWrongCharsetForReadingSourceFiles() {
    Project project = mock(Project.class);
    SensorContext context = mock(SensorContext.class);

    String encoding = CharEncoding.UTF_8;
    String testFile = "CP1252Encoding.java";
    fileEncodingTest(project, context, encoding, testFile);
  }

  private void fileEncodingTest(Project project, SensorContext context, String encoding, String testFile) {
    DefaultProjectFileSystem fileSystem = mock(DefaultProjectFileSystem.class);
    when(project.getFileSystem()).thenReturn(fileSystem);
    when(fileSystem.getSourceCharset()).thenReturn(Charset.forName(encoding));
    when(project.getConfiguration()).thenReturn(new MapConfiguration(new HashMap<String, String>()));
    when(fileSystem.getSourceFiles((Language) anyObject())).thenReturn(Arrays.asList(
        new File("test-resources/org/sonar/api/batch/AbstractSourceImporterTest/encoding/" + testFile)));

    importer.analyse(project, context);

    verify(context).saveSource(eq(FakeSourceImporter.TEST_RESOURCE), argThat(new BaseMatcher<String>() {

      public boolean matches(Object arg0) {
        String source = (String) arg0;
        return source.contains(aClaess) && source.contains(explicacao);
      }

      public void describeTo(Description arg0) {
      }
    }));
  }

  @Test
  public void testCreateUnitTestResource() {
    AbstractSourceImporter importer = new AbstractSourceImporter(Java.INSTANCE) {
    };

    File unitTestFile = new File("test/UnitTest.java");
    File unitTestDir = new File("test");
    List<File> unitTestDirs = Lists.newArrayList();
    unitTestDirs.add(unitTestDir);

    Resource unitTest = importer.createResource(unitTestFile, unitTestDirs, true);
    assertThat(unitTest.getQualifier(), is("UTS"));

    Resource srcTest = importer.createResource(unitTestFile, unitTestDirs, false);
    assertThat(srcTest.getQualifier(), is("FIL"));
  }

  private static class FakeSourceImporter extends AbstractSourceImporter {

    private final static Resource TEST_RESOURCE = new JavaFile("Test");

    private FakeSourceImporter() {
      super(null);
    }

    @Override
    protected Resource createResource(File file, List<File> sourceDirs, boolean unitTest) {
      return TEST_RESOURCE;
    }
  }

}
