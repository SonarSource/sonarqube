/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.anticipatedtransition;

import java.time.Instant;
import java.util.List;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.issue.AnticipatedTransitionDto;
import org.sonar.db.issue.AnticipatedTransitionMapper;

import static org.apache.commons.lang3.RandomStringUtils.secure;

public class AnticipatedTransitionDbTester {

  private final DbTester db;

  public AnticipatedTransitionDbTester(DbTester db) {
    this.db = db;
  }

  public AnticipatedTransitionDto createForIssue(DefaultIssue issue, String userUuid, String filePath) {
    var dto = new AnticipatedTransitionDto(
      "uuid_" + secure().nextAlphabetic(5),
      issue.projectUuid(),
      userUuid,
      "wontfix",
      "comment for transition",
      issue.getLine(),
      issue.getMessage(),
      null,
      issue.getRuleKey().rule(),
      filePath,
      Instant.now().toEpochMilli());
    db.getDbClient().anticipatedTransitionDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  public List<AnticipatedTransitionDto> selectByProjectUuid(String projectUuid) {
    try (DbSession session = db.getDbClient().openSession(false)) {
      AnticipatedTransitionMapper mapper = session.getMapper(AnticipatedTransitionMapper.class);
      return mapper.selectByProjectUuid(projectUuid);
    }
  }
}
