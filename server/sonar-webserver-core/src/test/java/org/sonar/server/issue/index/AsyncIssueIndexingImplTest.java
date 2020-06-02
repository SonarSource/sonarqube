/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.issue.index;

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.BranchType.BRANCH;

public class AsyncIssueIndexingImplTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public LogTester logTester = new LogTester();

  private DbClient dbClient = dbTester.getDbClient();
  private CeQueue ceQueue = mock(CeQueue.class);
  private UuidFactory uuidFactory = new SequenceUuidFactory();

  private final AsyncIssueIndexingImpl underTest = new AsyncIssueIndexingImpl(ceQueue, dbClient);

  @Before
  public void before() {
    when(ceQueue.prepareSubmit()).thenReturn(new CeTaskSubmit.Builder(uuidFactory.create()));
  }

  @Test
  public void triggerOnIndexCreation() {
    BranchDto dto = new BranchDto()
      .setBranchType(BRANCH)
      .setKey("branchName")
      .setUuid("branch_uuid")
      .setProjectUuid("project_uuid");
    dbClient.branchDao().insert(dbTester.getSession(), dto);
    dbTester.commit();

    underTest.triggerOnIndexCreation();

    Optional<BranchDto> branch = dbClient.branchDao().selectByUuid(dbTester.getSession(), "branch_uuid");
    assertThat(branch).isPresent();
    assertThat(branch.get().isNeedIssueSync()).isTrue();
    verify(ceQueue, times(1)).prepareSubmit();
    verify(ceQueue, times(1)).massSubmit(anyCollection());
    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("1 branch found in need of issue sync : BranchDto{uuid='branch_uuid', projectUuid='project_uuid'," +
        " kee='branchName', keyType=BRANCH, branchType=BRANCH, mergeBranchUuid='null', excludeFromPurge=false, needIssueSync=true}");
  }

  @Test
  public void triggerOnIndexCreation_no_branch() {
    underTest.triggerOnIndexCreation();

    assertThat(logTester.logs(LoggerLevel.INFO)).contains("No branch found in need of issue sync");
  }

}
