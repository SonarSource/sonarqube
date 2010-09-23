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

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.DefaultProjectFileSystem;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.*;
import org.sonar.api.test.IsViolation;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;

public class PmdViolationsXmlParserTest {

  private void parse(SensorContext context, String xmlPath) throws URISyntaxException, XMLStreamException {
    DefaultProjectFileSystem fileSystem = mock(DefaultProjectFileSystem.class);
    when(fileSystem.getSourceDirs()).thenReturn(Arrays.asList(new File("/test/src/main/java")));

    Project project = mock(Project.class);
    when(project.getFileSystem()).thenReturn(fileSystem);

    RulesManager manager = mock(RulesManager.class);
    when(manager.getPluginRule(anyString(), anyString())).thenAnswer(new Answer<Rule>() {
      public Rule answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        return new Rule((String) args[1], (String) args[1], null, (String) args[0], "");
      }
    });
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getActiveRule(anyString(), anyString())).thenReturn(new ActiveRule(null, null, RulePriority.MINOR));
    PmdViolationsXmlParser parser = new PmdViolationsXmlParser(project, context, manager, profile);

    File xmlFile = new File(getClass().getResource(xmlPath).toURI());
    parser.parse(xmlFile);
  }

  @Test
  public void shouldSaveViolationsOnClasses() throws URISyntaxException, XMLStreamException {
    SensorContext context = mock(SensorContext.class);
    parse(context, "/org/sonar/plugins/pmd/pmd-result.xml");

    verify(context, times(30)).saveViolation(argThat(new IsViolationOnJavaClass()));
    verify(context, times(4)).saveViolation(argThat(new IsViolationOnJavaClass(new JavaFile("ch.hortis.sonar.mvn.ClassWithComments"))));

    Violation wanted = Violation.create((Rule)null, new JavaFile("ch.hortis.sonar.mvn.ClassWithComments"))
        .setMessage("Avoid unused local variables such as 'toto'.")
        .setLineId(22);
    verify(context, times(1)).saveViolation(argThat(new IsViolation(wanted)));
  }

  @Test
  public void defaultPackageShouldBeSetOnclassWithoutPackage() throws URISyntaxException, XMLStreamException {
    SensorContext context = mock(SensorContext.class);
    parse(context, "/org/sonar/plugins/pmd/pmd-class-without-package.xml");
    verify(context, times(3)).saveViolation(argThat(new IsViolationOnJavaClass(new JavaFile("ClassOnDefaultPackage"))));
  }

  @Test
  public void unknownXMLEntity() throws URISyntaxException, XMLStreamException {
    SensorContext context = mock(SensorContext.class);
    parse(context, "/org/sonar/plugins/pmd/pmd-result-with-unknown-entity.xml");
    verify(context, times(2)).saveViolation(argThat(new IsViolationOnJavaClass(new JavaFile("test.Test"))));
  }


  @Test
  public void ISOControlCharsXMLFile() throws URISyntaxException, XMLStreamException {
    SensorContext context = mock(SensorContext.class);
    parse(context, "/org/sonar/plugins/pmd/pmd-result-with-control-char.xml");
    verify(context, times(1)).saveViolation(argThat(new IsViolationOnJavaClass(new JavaFile("test.Test"))));
  }


  private class IsViolationOnJavaClass extends BaseMatcher<Violation> {

    private JavaFile javaClass;

    private IsViolationOnJavaClass(JavaFile javaClass) {
      this.javaClass = javaClass;
    }

    private IsViolationOnJavaClass() {
    }

    public boolean matches(Object o) {
      Violation v = (Violation) o;
      boolean ok = (v.getResource() != null) && (v.getResource() instanceof JavaFile);
      if (ok && javaClass != null) {
        ok = javaClass.equals(v.getResource());
      }
      return ok;
    }

    public void describeTo(Description description) {

    }
  }
}
