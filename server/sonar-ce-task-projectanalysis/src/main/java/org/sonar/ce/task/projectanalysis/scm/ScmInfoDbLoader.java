/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.scm;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.source.OriginalFileResolver;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.source.FileSourceDto;

public class ScmInfoDbLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(ScmInfoDbLoader.class);

  private final DbClient dbClient;
  private final OriginalFileResolver originalFileResolver;

  public ScmInfoDbLoader(DbClient dbClient, OriginalFileResolver originalFileResolver) {
    this.dbClient = dbClient;
    this.originalFileResolver = originalFileResolver;
  }

  /**
   * Load DB-stored SCM info for {@code file}.
   *
   * <p>When {@code useReferenceBranchForNcd} is {@code true} (REFERENCE_BRANCH NCD with no SCM in the report)
   * the SCM info is read from the file's counterpart on the reference branch — so that downstream consumers
   * (notably {@code SourceLinesDiff}) can index into it consistently. When {@code false}, the usual
   * {@link OriginalFileResolver} logic is used.
   */
  public Optional<DbScmInfo> getScmInfo(Component file, boolean useReferenceBranchForNcd) {
    Optional<String> uuid = originalFileResolver.getFileUuid(file, useReferenceBranchForNcd);
    if (uuid.isEmpty()) {
      return Optional.empty();
    }

    LOGGER.trace("Reading SCM info from DB for file '{}'", uuid.get());
    try (DbSession dbSession = dbClient.openSession(false)) {
      FileSourceDto dto = dbClient.fileSourceDao().selectByFileUuid(dbSession, uuid.get());
      if (dto == null) {
        return Optional.empty();
      }
      return DbScmInfo.create(dto.getSourceData().getLinesList(), dto.getLineCount(), dto.getSrcHash());
    }
  }

}
