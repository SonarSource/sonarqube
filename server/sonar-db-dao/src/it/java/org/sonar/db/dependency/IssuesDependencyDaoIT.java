/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.dependency;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

class IssuesDependencyDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final IssuesDependencyDao issuesDependencyDao = db.getDbClient().issuesDependencyDao();

  @Test
  void insert_shouldPersistIssuesDependency() {
    var issuesDependencyDto = new IssuesDependencyDto("ISSUE_UUID", "CVE_UUID");

    issuesDependencyDao.insert(db.getSession(), issuesDependencyDto);

    List<Map<String, Object>> result = db.select(db.getSession(), "select * from issues_dependency");
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).containsExactlyInAnyOrderEntriesOf(
      Map.of(
        "issue_uuid", issuesDependencyDto.issueUuid(),
        "cve_uuid", issuesDependencyDto.cveUuid())
    );

  }
}
