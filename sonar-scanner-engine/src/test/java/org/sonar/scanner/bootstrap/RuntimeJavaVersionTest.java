/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.bootstrap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.event.Level;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.core.documentation.DocumentationLinkGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.scanner.bootstrap.RuntimeJavaVersion.WARNING_MESSAGE_TEMPLATE;

public class RuntimeJavaVersionTest {

  @Rule
  public LogTester logTester = new LogTester();

  private DocumentationLinkGenerator documentLinkGenerator = mock(DocumentationLinkGenerator.class);
  private AnalysisWarnings analysisWarnings = spy(AnalysisWarnings.class);
  private RuntimeJavaVersion underTest = new RuntimeJavaVersion(documentLinkGenerator, analysisWarnings);

  @Before
  public void before(){
    when(documentLinkGenerator.getDocumentationLink(any())).thenReturn("{}");
  }

  @Test
  public void given_runtime11_should_log_message() {
    try (MockedStatic<Runtime> utilities = Mockito.mockStatic(Runtime.class)) {
      Runtime.Version version = Runtime.Version.parse("11");
      utilities.when(Runtime::version).thenReturn(version);

      underTest.checkJavaVersion();

      assertThat(logTester.getLogs(Level.WARN)).extracting(LogAndArguments::getRawMsg)
        .anyMatch(s -> s.contains(RuntimeJavaVersion.LOG_MESSAGE));
    }
  }

  @Test
  public void given_runtime11_should_addAnalysisWarning() {
    try (MockedStatic<Runtime> utilities = Mockito.mockStatic(Runtime.class)) {
      Runtime.Version version = Runtime.Version.parse("11");
      utilities.when(Runtime::version).thenReturn(version);

      underTest.checkJavaVersion();

      verify(analysisWarnings).addUnique(WARNING_MESSAGE_TEMPLATE);
    }
  }


  @Test
  public void given_runtime17_should_notLogOrAddWarning() {
    try (MockedStatic<Runtime> utilities = Mockito.mockStatic(Runtime.class)) {
      Runtime.Version version = Runtime.Version.parse("17");
      utilities.when(Runtime::version).thenReturn(version);

      underTest.checkJavaVersion();

      verifyNoInteractions(analysisWarnings);
      assertThat(logTester.logs()).isEmpty();
    }
  }
  @Test

  public void given_runtime20_should_notLogOrAddWarning() {
    try (MockedStatic<Runtime> utilities = Mockito.mockStatic(Runtime.class)) {
      Runtime.Version version = Runtime.Version.parse("20");
      utilities.when(Runtime::version).thenReturn(version);

      underTest.checkJavaVersion();

      verifyNoInteractions(analysisWarnings);
      assertThat(logTester.logs()).isEmpty();
    }
  }
}
