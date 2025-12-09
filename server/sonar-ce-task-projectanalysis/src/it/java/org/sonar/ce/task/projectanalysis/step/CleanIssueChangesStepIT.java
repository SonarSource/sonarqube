/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.step;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.issue.IssueChangesToDeleteRepository;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

public class CleanIssueChangesStepIT {
  @Rule
  public DbTester db = DbTester.create();
  private final IssueChangesToDeleteRepository repository = new IssueChangesToDeleteRepository();
  private final CleanIssueChangesStep cleanIssueChangesStep = new CleanIssueChangesStep(repository, db.getDbClient());
  private final TestComputationStepContext context = new TestComputationStepContext();

  @Test
  public void steps_deletes_all_changes_in_repository() {
    IssueDto issue1 = db.issues().insert();
    IssueChangeDto change1 = db.issues().insertChange(issue1);
    IssueChangeDto change2 = db.issues().insertChange(issue1);

    repository.add(change1.getUuid());

    cleanIssueChangesStep.execute(context);
    assertThat(db.getDbClient().issueChangeDao().selectByIssueKeys(db.getSession(), singleton(issue1.getKey())))
      .extracting(IssueChangeDto::getUuid)
      .containsOnly(change2.getUuid());
  }

  @Test
  public void steps_does_nothing_if_no_uuid() {
    IssueDto issue1 = db.issues().insert();
    IssueChangeDto change1 = db.issues().insertChange(issue1);
    IssueChangeDto change2 = db.issues().insertChange(issue1);

    cleanIssueChangesStep.execute(context);

    assertThat(db.getDbClient().issueChangeDao().selectByIssueKeys(db.getSession(), singleton(issue1.getKey())))
      .extracting(IssueChangeDto::getUuid)
      .containsOnly(change1.getUuid(), change2.getUuid());
  }
}
