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
package org.sonar.scanner.scm;

import java.util.Date;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.utils.System2;
import org.sonar.core.documentation.DocumentationLinkGenerator;
import org.sonar.scanner.notifications.DefaultAnalysisWarnings;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.scanner.scm.DefaultBlameOutput.SCM_INTEGRATION_DOCUMENTATION_SUFFIX;

public class DefaultBlameOutputTest {

  private static final String DOC_LINK = "https://link.to.documentation/new/page";
  private System2 system2 = mock(System2.class);
  private DefaultAnalysisWarnings analysisWarnings = new DefaultAnalysisWarnings(system2);
  private DocumentationLinkGenerator documentationLinkGenerator = mock(DocumentationLinkGenerator.class);

  @Test
  public void shouldNotFailIfNotSameNumberOfLines() {
    InputFile file = new TestInputFileBuilder("foo", "src/main/java/Foo.java").setLines(10).build();

    new DefaultBlameOutput(null, analysisWarnings, singletonList(file), mock(DocumentationLinkGenerator.class)).blameResult(file,
      singletonList(new BlameLine().revision("1").author("guy")));
  }

  @Test
  public void addWarningIfFilesMissing() {
    when(documentationLinkGenerator.getDocumentationLink(SCM_INTEGRATION_DOCUMENTATION_SUFFIX)).thenReturn(DOC_LINK);
    InputFile file = new TestInputFileBuilder("foo", "src/main/java/Foo.java").setLines(10).build();
    new DefaultBlameOutput(null, analysisWarnings, singletonList(file), documentationLinkGenerator).finish(true);
    assertThat(analysisWarnings.warnings()).extracting(DefaultAnalysisWarnings.Message::getText)
      .containsOnly("Missing blame information for 1 file. This may lead to some features not working correctly. " +
        "Please check the analysis logs and refer to <a href=\"" + DOC_LINK + "\" rel=\"noopener noreferrer\" target=\"_blank\">the documentation</a>.");
  }

  @Test
  public void addWarningWithContextIfFiles() {
    when(documentationLinkGenerator.getDocumentationLink(SCM_INTEGRATION_DOCUMENTATION_SUFFIX)).thenReturn(DOC_LINK);
    InputFile file = new TestInputFileBuilder("foo", "src/main/java/Foo.java").setLines(10).build();
    when(documentationLinkGenerator.getDocumentationLink("suffix")).thenReturn("blababl");
    new DefaultBlameOutput(null, analysisWarnings, singletonList(file), documentationLinkGenerator).finish(true);
    assertThat(analysisWarnings.warnings()).extracting(DefaultAnalysisWarnings.Message::getText)
      .containsOnly("Missing blame information for 1 file. This may lead to some features not working correctly. " +
        "Please check the analysis logs and refer to <a href=\"" + DOC_LINK + "\" rel=\"noopener noreferrer\" target=\"_blank\">the documentation</a>.");
  }

  @Test
  public void shouldFailIfNotExpectedFile() {
    InputFile file = new TestInputFileBuilder("foo", "src/main/java/Foo.java").build();
    var blameOutput = new DefaultBlameOutput(null, analysisWarnings,
      singletonList(new TestInputFileBuilder("foo", "src/main/java/Foo2.java").build()),
      mock(DocumentationLinkGenerator.class));
    var lines = singletonList(new BlameLine().revision("1").author("guy"));

    assertThatThrownBy(() -> blameOutput.blameResult(file, lines))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("It was not expected to blame file " + file);
  }

  @Test
  public void shouldFailIfNullDate() {
    InputFile file = new TestInputFileBuilder("foo", "src/main/java/Foo.java").setLines(1).build();
    var blameOutput = new DefaultBlameOutput(null, analysisWarnings, singletonList(file), mock(DocumentationLinkGenerator.class));
    var lines = singletonList(new BlameLine().revision("1").author("guy"));

    assertThatThrownBy(() -> blameOutput.blameResult(file, lines))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Blame date is null for file " + file + " at line 1");
  }

  @Test
  public void shouldFailIfNullRevision() {
    InputFile file = new TestInputFileBuilder("foo", "src/main/java/Foo.java").setLines(1).build();
    var blameOUtput = new DefaultBlameOutput(null, analysisWarnings, singletonList(file), mock(DocumentationLinkGenerator.class));
    var lines = singletonList(new BlameLine().date(new Date()).author("guy"));

    assertThatThrownBy(() -> blameOUtput.blameResult(file, lines))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Blame revision is blank for file " + file + " at line 1");
  }
}
