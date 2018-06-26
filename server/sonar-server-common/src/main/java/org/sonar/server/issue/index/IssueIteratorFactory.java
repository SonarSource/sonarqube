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
package org.sonar.server.issue.index;

import java.util.Collection;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;

public class IssueIteratorFactory {

  private final DbClient dbClient;

  public IssueIteratorFactory(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public IssueIterator createForAll() {
    return createForProject(null);
  }

  public IssueIterator createForProject(@Nullable String projectUuid) {
    return new IssueIteratorForSingleChunk(dbClient, projectUuid, null);
  }

  public IssueIterator createForIssueKeys(Collection<String> issueKeys) {
    return new IssueIteratorForMultipleChunks(dbClient, issueKeys);
  }
}
