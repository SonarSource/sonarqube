/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.postjob;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.postjob.issue.Issue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.File;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.issue.IssueCache;
import org.sonar.core.issue.DefaultIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultPostJobContextTest {

  private IssueCache issueCache;
  private BatchComponentCache resourceCache;
  private AnalysisMode analysisMode;
  private DefaultPostJobContext context;
  private Settings settings;

  @Before
  public void prepare() {
    issueCache = mock(IssueCache.class);
    resourceCache = new BatchComponentCache();
    analysisMode = mock(AnalysisMode.class);
    settings = new Settings();
    context = new DefaultPostJobContext(settings, analysisMode, issueCache, resourceCache);
  }

  @Test
  public void test() {
    assertThat(context.settings()).isSameAs(settings);
    assertThat(context.analysisMode()).isSameAs(analysisMode);

    DefaultIssue defaultIssue = new DefaultIssue();
    defaultIssue.setComponentKey("foo:src/Foo.php");
    defaultIssue.setEffortToFix(2.0);
    defaultIssue.setNew(true);
    defaultIssue.setKey("xyz");
    defaultIssue.setLine(1);
    defaultIssue.setMessage("msg");
    defaultIssue.setSeverity("BLOCKER");
    when(issueCache.all()).thenReturn(Arrays.asList(defaultIssue));

    Issue issue = context.issues().iterator().next();
    assertThat(issue.componentKey()).isEqualTo("foo:src/Foo.php");
    assertThat(issue.effortToFix()).isEqualTo(2.0);
    assertThat(issue.isNew()).isTrue();
    assertThat(issue.key()).isEqualTo("xyz");
    assertThat(issue.line()).isEqualTo(1);
    assertThat(issue.message()).isEqualTo("msg");
    assertThat(issue.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(issue.inputComponent()).isNull();

    InputFile inputPath = mock(InputFile.class);
    resourceCache.add(File.create("src/Foo.php").setEffectiveKey("foo:src/Foo.php"), null).setInputComponent(inputPath);
    assertThat(issue.inputComponent()).isEqualTo(inputPath);

  }
}
