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

package org.sonar.batch.issue.tracking;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.batch.bootstrap.BootstrapProperties;
import org.sonar.batch.bootstrap.TempFolderProvider;
import org.sonar.batch.index.Caches;
import org.sonar.core.issue.db.IssueChangeDto;
import org.sonar.core.issue.db.IssueDto;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class InitialOpenIssuesStackTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  public static Caches createCacheOnTemp(TemporaryFolder temp) {
    BootstrapProperties bootstrapSettings = new BootstrapProperties(Collections.<String, String>emptyMap());
    try {
      bootstrapSettings.properties().put(CoreProperties.WORKING_DIRECTORY, temp.newFolder().getAbsolutePath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new Caches(new TempFolderProvider().provide(bootstrapSettings));
  }

  InitialOpenIssuesStack stack;
  Caches caches;

  @Before
  public void before() throws Exception {
    caches = createCacheOnTemp(temp);
    caches.start();
    stack = new InitialOpenIssuesStack(caches);
  }

  @After
  public void after() {
    caches.stop();
  }

  @Test
  public void get_and_remove_issues() {
    IssueDto issueDto = new IssueDto().setComponentKey("org.struts.Action").setKee("ISSUE-1");
    stack.addIssue(issueDto);

    List<ServerIssue> issueDtos = stack.selectAndRemoveIssues("org.struts.Action");
    assertThat(issueDtos).hasSize(1);
    assertThat(issueDtos.get(0).key()).isEqualTo("ISSUE-1");

    assertThat(stack.selectAllIssues()).isEmpty();
  }

  @Test
  public void get_and_remove_with_many_issues_on_same_resource() {
    stack.addIssue(new IssueDto().setComponentKey("org.struts.Action").setKee("ISSUE-1"));
    stack.addIssue(new IssueDto().setComponentKey("org.struts.Action").setKee("ISSUE-2"));

    List<ServerIssue> issueDtos = stack.selectAndRemoveIssues("org.struts.Action");
    assertThat(issueDtos).hasSize(2);

    assertThat(stack.selectAllIssues()).isEmpty();
  }

  @Test
  public void get_and_remove_do_nothing_if_resource_not_found() {
    stack.addIssue(new IssueDto().setComponentKey("org.struts.Action").setKee("ISSUE-1"));

    List<ServerIssue> issueDtos = stack.selectAndRemoveIssues("Other");
    assertThat(issueDtos).hasSize(0);

    assertThat(stack.selectAllIssues()).hasSize(1);
  }

  @Test
  public void select_changelog() {
    stack.addChangelog(new IssueChangeDto().setKey("CHANGE-1").setIssueKey("ISSUE-1"));
    stack.addChangelog(new IssueChangeDto().setKey("CHANGE-2").setIssueKey("ISSUE-1"));

    List<IssueChangeDto> issueChangeDtos = stack.selectChangelog("ISSUE-1");
    assertThat(issueChangeDtos).hasSize(2);
    assertThat(issueChangeDtos.get(0).getKey()).isEqualTo("CHANGE-1");
    assertThat(issueChangeDtos.get(1).getKey()).isEqualTo("CHANGE-2");
  }

  @Test
  public void return_empty_changelog() {
    assertThat(stack.selectChangelog("ISSUE-1")).isEmpty();
  }

  @Test
  public void clear_issues() {
    stack.addIssue(new IssueDto().setComponentKey("org.struts.Action").setKee("ISSUE-1"));

    assertThat(stack.selectAllIssues()).hasSize(1);

    // issues are not removed
    assertThat(stack.selectAllIssues()).hasSize(1);

    stack.clear();
    assertThat(stack.selectAllIssues()).isEmpty();
  }

  @Test
  public void clear_issues_changelog() {
    stack.addChangelog(new IssueChangeDto().setKey("CHANGE-1").setIssueKey("ISSUE-1"));

    assertThat(stack.selectChangelog("ISSUE-1")).hasSize(1);

    stack.clear();
    assertThat(stack.selectChangelog("ISSUE-1")).isEmpty();
  }
}
