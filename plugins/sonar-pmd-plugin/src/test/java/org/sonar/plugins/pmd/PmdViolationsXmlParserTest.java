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

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;
import org.sonar.api.test.IsViolation;

import java.io.File;
import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class PmdViolationsXmlParserTest {

  private void parse(SensorContext context, String xmlPath, boolean useIndexedResources) throws Exception {
    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    when(fileSystem.getSourceDirs()).thenReturn(Arrays.asList(new File("/test/src/main/java")));

    Project project = mock(Project.class);
    when(project.getFileSystem()).thenReturn(fileSystem);

    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findByKey(anyString(), anyString())).thenAnswer(new Answer<Rule>() {
      public Rule answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return Rule.create((String) args[0], (String) args[1], "");
      }
    });

    if (useIndexedResources) {
      when(context.getResource((JavaFile) any())).thenReturn(new JavaFile(""));
    } else {
      when(context.getResource((JavaFile) any())).thenReturn(null);
    }

    PmdViolationsXmlParser parser = new PmdViolationsXmlParser(project, ruleFinder, context);

    File xmlFile = new File(getClass().getResource(xmlPath).toURI());
    parser.parse(xmlFile);
  }

  @Test
  public void shouldSaveViolationsOnFiles() throws Exception {
    SensorContext context = mock(SensorContext.class);
    parse(context, "/org/sonar/plugins/pmd/pmd-result.xml", true);

    verify(context, times(30)).saveViolation(argThat(new IsViolationOnJavaFile()));
    verify(context, times(4)).saveViolation(argThat(new IsViolationOnJavaFile(new JavaFile("ch.hortis.sonar.mvn.ClassWithComments"))));

    Violation wanted = Violation.create((Rule) null, new JavaFile("ch.hortis.sonar.mvn.ClassWithComments"))
      .setMessage("Avoid unused local variables such as 'toto'.")
      .setLineId(22);
    verify(context, times(1)).saveViolation(argThat(new IsViolation(wanted)));
  }

  @Test
  public void shouldIgnoreNonIndexedResources() throws Exception {
    SensorContext context = mock(SensorContext.class);
    parse(context, "/org/sonar/plugins/pmd/pmd-result.xml", false);

    verify(context, never()).saveViolation(argThat(new IsViolationOnJavaFile()));
  }

  @Test
  public void defaultPackageShouldBeSetOnClassWithoutPackage() throws Exception {
    SensorContext context = mock(SensorContext.class);
    parse(context, "/org/sonar/plugins/pmd/pmd-class-without-package.xml", true);
    verify(context, times(3)).saveViolation(argThat(new IsViolationOnJavaFile(new JavaFile("ClassOnDefaultPackage"))));
  }

  @Test
  public void unknownXMLEntity() throws Exception {
    SensorContext context = mock(SensorContext.class);
    parse(context, "/org/sonar/plugins/pmd/pmd-result-with-unknown-entity.xml", true);
    verify(context, times(2)).saveViolation(argThat(new IsViolationOnJavaFile(new JavaFile("test.Test"))));
  }

  @Test
  public void ISOControlCharsXMLFile() throws Exception {
    SensorContext context = mock(SensorContext.class);
    parse(context, "/org/sonar/plugins/pmd/pmd-result-with-control-char.xml", true);
    verify(context, times(1)).saveViolation(argThat(new IsViolationOnJavaFile(new JavaFile("test.Test"))));
  }

  private class IsViolationOnJavaFile extends BaseMatcher<Violation> {
    private JavaFile javaFile;

    private IsViolationOnJavaFile(JavaFile javaFile) {
      this.javaFile = javaFile;
    }

    private IsViolationOnJavaFile() {
    }

    public boolean matches(Object o) {
      Violation v = (Violation) o;
      boolean ok = (v.getResource() != null) && (v.getResource() instanceof JavaFile);
      if (ok && javaFile != null) {
        ok = javaFile.equals(v.getResource());
      }
      return ok;
    }

    public void describeTo(Description description) {

    }
  }
}
