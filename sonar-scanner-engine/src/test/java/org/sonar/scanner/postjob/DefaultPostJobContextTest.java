/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.postjob;

import java.io.IOException;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.scanner.issue.IssueCache;
import org.sonar.scanner.issue.tracking.TrackedIssue;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultPostJobContextTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private IssueCache issueCache;
  private InputComponentStore componentStore;
  private DefaultPostJobContext context;
  private MapSettings settings;
  private AnalysisMode analysisMode;

  @Before
  public void setUp() throws IOException {
    issueCache = mock(IssueCache.class);
    DefaultInputModule rootModule = TestInputFileBuilder.newDefaultInputModule("foo", temp.newFolder());
    componentStore = new InputComponentStore(rootModule, mock(BranchConfiguration.class));
    settings = new MapSettings();
    analysisMode = mock(AnalysisMode.class);
    context = new DefaultPostJobContext(settings.asConfig(), settings, issueCache, componentStore, analysisMode);
  }

  @Test
  public void testIssues() throws IOException {
    when(analysisMode.isIssues()).thenReturn(true);

    assertThat(context.settings()).isSameAs(settings);

    TrackedIssue defaultIssue = new TrackedIssue();
    defaultIssue.setComponentKey("foo:src/Foo.php");
    defaultIssue.setGap(2.0);
    defaultIssue.setNew(true);
    defaultIssue.setKey("xyz");
    defaultIssue.setStartLine(1);
    defaultIssue.setMessage("msg");
    defaultIssue.setSeverity("BLOCKER");
    when(issueCache.all()).thenReturn(Arrays.asList(defaultIssue));

    PostJobIssue issue = context.issues().iterator().next();
    assertThat(issue.componentKey()).isEqualTo("foo:src/Foo.php");
    assertThat(issue.isNew()).isTrue();
    assertThat(issue.key()).isEqualTo("xyz");
    assertThat(issue.line()).isEqualTo(1);
    assertThat(issue.message()).isEqualTo("msg");
    assertThat(issue.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(issue.inputComponent()).isNull();

    String moduleKey = "foo";
    componentStore.put(new TestInputFileBuilder(moduleKey, "src/Foo.php").build());
    assertThat(issue.inputComponent()).isNotNull();

  }
}
