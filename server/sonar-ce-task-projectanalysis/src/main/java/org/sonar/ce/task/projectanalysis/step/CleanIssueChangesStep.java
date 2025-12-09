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

import java.util.Set;
import org.sonar.ce.task.projectanalysis.issue.IssueChangesToDeleteRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

public class CleanIssueChangesStep implements ComputationStep {
  private final IssueChangesToDeleteRepository issueChangesToDeleteRepository;
  private final DbClient dbClient;

  public CleanIssueChangesStep(IssueChangesToDeleteRepository issueChangesToDeleteRepository, DbClient dbClient) {
    this.issueChangesToDeleteRepository = issueChangesToDeleteRepository;
    this.dbClient = dbClient;
  }

  @Override
  public void execute(Context context) {
    Set<String> uuids = issueChangesToDeleteRepository.getUuids();
    context.getStatistics().add("changes", uuids.size());

    if (uuids.isEmpty()) {
      return;
    }

    try (DbSession dbSession = dbClient.openSession(false)) {

      dbClient.issueChangeDao().deleteByUuids(dbSession, issueChangesToDeleteRepository.getUuids());
      dbSession.commit();
    }
  }

  @Override
  public String getDescription() {
    return "Delete issue changes";
  }
}
