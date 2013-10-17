/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.plugins.core.issue;

import edu.emory.mathcs.backport.java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.batch.bootstrap.BootstrapProperties;
import org.sonar.batch.bootstrap.BootstrapSettings;
import org.sonar.batch.bootstrap.TempFolderProvider;
import org.sonar.batch.index.Caches;
import org.sonar.core.issue.db.IssueDto;

import java.io.IOException;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class InitialOpenIssuesStackTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  public static Caches createCacheOnTemp(TemporaryFolder temp) {
    BootstrapSettings bootstrapSettings = new BootstrapSettings(new BootstrapProperties(Collections.emptyMap()));
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
  public void setUp() throws Exception {
    caches = createCacheOnTemp(temp);
    caches.start();
    stack = new InitialOpenIssuesStack(caches);
  }

  @After
  public void tearDown() {
    caches.stop();
  }

  @Test
  public void should_get_and_remove() {
    IssueDto issueDto = new IssueDto().setComponentKey_unit_test_only("org.struts.Action").setKee("ISSUE-1");
    stack.addIssue(issueDto);

    List<IssueDto> issueDtos = stack.selectAndRemove("org.struts.Action");
    assertThat(issueDtos).hasSize(1);
    assertThat(issueDtos.get(0).getKee()).isEqualTo("ISSUE-1");

    assertThat(stack.selectAll()).isEmpty();
  }

  @Test
  public void should_get_and_remove_with_many_issues_on_same_resource() {
    stack.addIssue(new IssueDto().setComponentKey_unit_test_only("org.struts.Action").setKee("ISSUE-1"));
    stack.addIssue(new IssueDto().setComponentKey_unit_test_only("org.struts.Action").setKee("ISSUE-2"));

    List<IssueDto> issueDtos = stack.selectAndRemove("org.struts.Action");
    assertThat(issueDtos).hasSize(2);

    assertThat(stack.selectAll()).isEmpty();
  }

  @Test
  public void should_do_nothing_if_resource_not_found() {
    stack.addIssue(new IssueDto().setComponentKey_unit_test_only("org.struts.Action").setKee("ISSUE-1"));

    List<IssueDto> issueDtos = stack.selectAndRemove("Other");
    assertThat(issueDtos).hasSize(0);

    assertThat(stack.selectAll()).hasSize(1);
  }

  @Test
  public void should_clear() {
    stack.addIssue(new IssueDto().setComponentKey_unit_test_only("org.struts.Action").setKee("ISSUE-1"));

    assertThat(stack.selectAll()).hasSize(1);

    // issues are not removed
    assertThat(stack.selectAll()).hasSize(1);

    stack.clear();
    assertThat(stack.selectAll()).hasSize(0);
  }
}
