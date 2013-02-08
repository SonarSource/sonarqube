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
package org.sonar.api.batch;

import com.google.common.io.Files;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.CharEncoding;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.exceptions.verification.junit.ArgumentsAreDifferent;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractSourceImporterTest {

  private String aClaess;
  private String explicacao;
  private FakeSourceImporter importer;

  @Before
  public void setup() throws UnsupportedEncodingException {
    aClaess = new String(new byte[] {65, 67, 108, 97, -61, -88, 115, 115, 40, 41}, CharEncoding.UTF_8);
    explicacao = new String(new byte[] {101, 120, 112, 108, 105, 99, 97, -61, -89, -61, -93, 111, 40, 41}, CharEncoding.UTF_8);
    importer = new FakeSourceImporter();
  }

  @Test
  public void shouldBeEnabledByDefault() {
    Project pom = mock(Project.class);
    when(pom.getConfiguration()).thenReturn(new PropertiesConfiguration());
    assertTrue(importer.isEnabled(pom));
  }

  @Test
  public void do_not_save_source_if_null_resource() {
    AbstractSourceImporter nullImporter = new AbstractSourceImporter(Java.INSTANCE) {
      @Override
      protected Resource createResource(File file, List<File> sourceDirs, boolean unitTest) {
        return null;
      }
    };

    SensorContext context = mock(SensorContext.class);
    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    when(fileSystem.getSourceFiles(Java.INSTANCE)).thenReturn(newArrayList(new File("Foo.java"), new File("Bar.java")));
    nullImporter.analyse(fileSystem, context);

    verify(context, never()).saveSource(any(Resource.class), anyString());
  }

  @Test
  public void should_use_mac_roman_charset_forR_reading_source_files() throws Exception {
    Project project = mock(Project.class);
    SensorContext context = mock(SensorContext.class);

    String encoding = "MacRoman";
    String testFile = "MacRomanEncoding.java";
    fileEncodingTest(project, context, encoding, testFile);
  }

  @Test
  public void should_use_CP1252_charset_for_reading_source_files() throws Exception {
    Project project = mock(Project.class);
    SensorContext context = mock(SensorContext.class);

    String encoding = "CP1252";
    String testFile = "CP1252Encoding.java";
    fileEncodingTest(project, context, encoding, testFile);
  }

  @Test(expected = ArgumentsAreDifferent.class)
  public void should_fail_with_wrong_charset_for_reading_source_files() throws Exception {
    Project project = mock(Project.class);
    SensorContext context = mock(SensorContext.class);

    String encoding = CharEncoding.UTF_8;
    String testFile = "CP1252Encoding.java";
    fileEncodingTest(project, context, encoding, testFile);
  }

  @Test
  public void should_remove_byte_order_mark_character() throws Exception {
    Project project = mock(Project.class);
    SensorContext context = mock(SensorContext.class);

    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    when(project.getFileSystem()).thenReturn(fileSystem);
    when(fileSystem.getSourceCharset()).thenReturn(Charset.forName(CharEncoding.UTF_8));
    when(project.getConfiguration()).thenReturn(new MapConfiguration(new HashMap<String, String>()));

    File file = new File(Files.createTempDir(), "Test.java");
    Files.write("\uFEFFpublic class Test", file, Charset.defaultCharset());
    when(fileSystem.getSourceFiles(any(Language.class))).thenReturn(newArrayList(file));

    importer.shouldExecuteOnProject(project);
    importer.analyse(project, context);

    verify(context).saveSource(eq(FakeSourceImporter.TEST_RESOURCE), argThat(new ArgumentMatcher<String>() {
      @Override
      public boolean matches(Object arg0) {
        String source = (String) arg0;
        return !source.contains("\uFEFF");
      }
    }));
  }

  private void fileEncodingTest(Project project, SensorContext context, String encoding, String testFile) throws Exception {
    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    when(project.getFileSystem()).thenReturn(fileSystem);
    when(fileSystem.getSourceCharset()).thenReturn(Charset.forName(encoding));
    when(project.getConfiguration()).thenReturn(new MapConfiguration(new HashMap<String, String>()));
    when(fileSystem.getSourceFiles(any(Language.class))).thenReturn(newArrayList(getFile(testFile)));

    importer.shouldExecuteOnProject(project);
    importer.analyse(project, context);

    verify(context).saveSource(eq(FakeSourceImporter.TEST_RESOURCE), argThat(new ArgumentMatcher<String>() {
      @Override
      public boolean matches(Object arg0) {
        String source = (String) arg0;
        return source.contains(aClaess) && source.contains(explicacao);
      }
    }));
  }

  @Test
  public void test_create_unit_test_resource() {
    AbstractSourceImporter importer = new AbstractSourceImporter(Java.INSTANCE) {
    };

    File unitTestFile = new File("test/UnitTest.java");
    File unitTestDir = new File("test");
    List<File> unitTestDirs = newArrayList();
    unitTestDirs.add(unitTestDir);

    Resource unitTest = importer.createResource(unitTestFile, unitTestDirs, true);
    assertThat(unitTest.getQualifier(), is("UTS"));

    Resource srcTest = importer.createResource(unitTestFile, unitTestDirs, false);
    assertThat(srcTest.getQualifier(), is("FIL"));
  }

  private static class FakeSourceImporter extends AbstractSourceImporter {

    private final static Resource TEST_RESOURCE = new JavaFile("Test");

    private FakeSourceImporter() {
      super(Java.INSTANCE);
    }

    @Override
    protected Resource createResource(File file, List<File> sourceDirs, boolean unitTest) {
      return TEST_RESOURCE;
    }
  }

  private File getFile(String testFile){
    return new File("test-resources/org/sonar/api/batch/AbstractSourceImporterTest/encoding/" + testFile);
  }

}
