/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.source;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;

import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

public class PersistFileSourcesStep implements ComputationStep {
  private final DbClient dbClient;
  private final System2 system2;
  private final TreeRootHolder treeRootHolder;
  private final SourceLinesHashRepository sourceLinesHash;
  private final FileSourceDataComputer fileSourceDataComputer;
  private final FileSourceDataWarnings fileSourceDataWarnings;

  public PersistFileSourcesStep(DbClient dbClient, System2 system2, TreeRootHolder treeRootHolder,
    SourceLinesHashRepository sourceLinesHash, FileSourceDataComputer fileSourceDataComputer,
    FileSourceDataWarnings fileSourceDataWarnings) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.treeRootHolder = treeRootHolder;
    this.sourceLinesHash = sourceLinesHash;
    this.fileSourceDataComputer = fileSourceDataComputer;
    this.fileSourceDataWarnings = fileSourceDataWarnings;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    // Don't use batch insert for file_sources since keeping all data in memory can produce OOM for big files
    try (DbSession dbSession = dbClient.openSession(false)) {
      new DepthTraversalTypeAwareCrawler(new FileSourceVisitor(dbSession))
        .visit(treeRootHolder.getRoot());
    } finally {
      fileSourceDataWarnings.commitWarnings();
    }
  }

  private class FileSourceVisitor extends TypeAwareVisitorAdapter {
    private final DbSession session;

    private Map<String, FileSourceDto> previousFileSourcesByUuid = new HashMap<>();
    private String projectUuid;

    private FileSourceVisitor(DbSession session) {
      super(CrawlerDepthLimit.FILE, PRE_ORDER);
      this.session = session;
    }

    @Override
    public void visitProject(Component project) {
      this.projectUuid = project.getUuid();
      session.select("org.sonar.db.source.FileSourceMapper.selectHashesForProject", ImmutableMap.of("projectUuid", projectUuid),
        context -> {
          FileSourceDto dto = (FileSourceDto) context.getResultObject();
          previousFileSourcesByUuid.put(dto.getFileUuid(), dto);
        });
    }

    @Override
    public void visitFile(Component file) {
      try {
        FileSourceDataComputer.Data fileSourceData = fileSourceDataComputer.compute(file, fileSourceDataWarnings);
        persistSource(fileSourceData, file);
      } catch (Exception e) {
        throw new IllegalStateException(String.format("Cannot persist sources of %s", file.getDbKey()), e);
      }
    }

    private void persistSource(FileSourceDataComputer.Data fileSourceData, Component file) {
      DbFileSources.Data lineData = fileSourceData.getLineData();

      byte[] binaryData = FileSourceDto.encodeSourceData(lineData);
      String dataHash = DigestUtils.md5Hex(binaryData);
      String srcHash = fileSourceData.getSrcHash();
      List<String> lineHashes = fileSourceData.getLineHashes();
      Changeset latestChangeWithRevision = fileSourceData.getLatestChangeWithRevision();
      int lineHashesVersion = sourceLinesHash.getLineHashesVersion(file);
      FileSourceDto previousDto = previousFileSourcesByUuid.get(file.getUuid());

      if (previousDto == null) {
        FileSourceDto dto = new FileSourceDto()
          .setProjectUuid(projectUuid)
          .setFileUuid(file.getUuid())
          .setBinaryData(binaryData)
          .setSrcHash(srcHash)
          .setDataHash(dataHash)
          .setLineHashes(lineHashes)
          .setLineHashesVersion(lineHashesVersion)
          .setCreatedAt(system2.now())
          .setUpdatedAt(system2.now())
          .setRevision(computeRevision(latestChangeWithRevision));
        dbClient.fileSourceDao().insert(session, dto);
        session.commit();
      } else {
        // Update only if data_hash has changed or if src_hash is missing or revision is missing (progressive migration)
        boolean binaryDataUpdated = !dataHash.equals(previousDto.getDataHash());
        boolean srcHashUpdated = !srcHash.equals(previousDto.getSrcHash());
        String revision = computeRevision(latestChangeWithRevision);
        boolean revisionUpdated = !ObjectUtils.equals(revision, previousDto.getRevision());
        boolean lineHashesVersionUpdated = previousDto.getLineHashesVersion() != lineHashesVersion;
        if (binaryDataUpdated || srcHashUpdated || revisionUpdated || lineHashesVersionUpdated) {
          previousDto
            .setBinaryData(binaryData)
            .setDataHash(dataHash)
            .setSrcHash(srcHash)
            .setLineHashes(lineHashes)
            .setLineHashesVersion(lineHashesVersion)
            .setRevision(revision)
            .setUpdatedAt(system2.now());
          dbClient.fileSourceDao().update(session, previousDto);
          session.commit();
        }
      }
    }

    @CheckForNull
    private String computeRevision(@Nullable Changeset latestChangeWithRevision) {
      if (latestChangeWithRevision == null) {
        return null;
      }
      return latestChangeWithRevision.getRevision();
    }
  }

  @Override
  public String getDescription() {
    return "Persist sources";
  }
}
