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
package org.sonar.batch.index;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.utils.System2;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.scan.filesystem.InputFileMetadata;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.core.source.db.FileSourceMapper;

import javax.annotation.CheckForNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class SourcePersister implements ScanPersister {

  private final MyBatis mybatis;
  private final System2 system2;
  private final ProjectTree projectTree;
  private final ResourceCache resourceCache;
  private final InputPathCache inputPathCache;
  private final SourceDataFactory dataFactory;

  public SourcePersister(InputPathCache inputPathCache, MyBatis mybatis, System2 system2,
    ProjectTree projectTree, ResourceCache resourceCache, SourceDataFactory dataFactory) {
    this.inputPathCache = inputPathCache;
    this.mybatis = mybatis;
    this.system2 = system2;
    this.projectTree = projectTree;
    this.resourceCache = resourceCache;
    this.dataFactory = dataFactory;
  }

  @Override
  public void persist() {
    // Don't use batch insert for file_sources since keeping all data in memory can produce OOM for big files
    try (DbSession session = mybatis.openSession(false)) {

      final Map<String, FileSourceDto> previousDtosByUuid = new HashMap<>();
      session.select("org.sonar.core.source.db.FileSourceMapper.selectHashesForProject", projectTree.getRootProject().getUuid(), new ResultHandler() {
        @Override
        public void handleResult(ResultContext context) {
          FileSourceDto dto = (FileSourceDto) context.getResultObject();
          previousDtosByUuid.put(dto.getFileUuid(), dto);
        }
      });

      FileSourceMapper mapper = session.getMapper(FileSourceMapper.class);
      for (InputFile inputFile : inputPathCache.allFiles()) {
        persist(session, mapper, (DefaultInputFile) inputFile, previousDtosByUuid);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Unable to save file sources", e);
    }

  }

  private void persist(DbSession session, FileSourceMapper mapper, DefaultInputFile inputFile, Map<String, FileSourceDto> previousDtosByUuid) {
    String fileUuid = resourceCache.get(inputFile.key()).resource().getUuid();

    InputFileMetadata metadata = inputPathCache.getFileMetadata(inputFile.moduleKey(), inputFile.relativePath());
    byte[] data = computeData(inputFile, metadata);
    String dataHash = DigestUtils.md5Hex(data);
    FileSourceDto previousDto = previousDtosByUuid.get(fileUuid);
    if (previousDto == null) {
      FileSourceDto dto = new FileSourceDto()
        .setProjectUuid(projectTree.getRootProject().getUuid())
        .setFileUuid(fileUuid)
        .setBinaryData(data)
        .setDataHash(dataHash)
        .setSrcHash(metadata.hash())
        .setLineHashes(lineHashesAsMd5Hex(inputFile, metadata))
        .setCreatedAt(system2.now())
        .setUpdatedAt(system2.now());
      mapper.insert(dto);
      session.commit();
    } else {
      // Update only if data_hash has changed or if src_hash is missing (progressive migration)
      if (!dataHash.equals(previousDto.getDataHash()) || !metadata.hash().equals(previousDto.getSrcHash())) {
        previousDto
          .setBinaryData(data)
          .setDataHash(dataHash)
          .setSrcHash(metadata.hash())
          .setLineHashes(lineHashesAsMd5Hex(inputFile, metadata))
          .setUpdatedAt(system2.now());
        mapper.update(previousDto);
        session.commit();
      }
    }
  }

  @CheckForNull
  private String lineHashesAsMd5Hex(DefaultInputFile f, InputFileMetadata metadata) {
    if (f.lines() == 0) {
      return null;
    }
    // A md5 string is 32 char long + '\n' = 33
    StringBuilder result = new StringBuilder(f.lines() * (32 + 1));

    try {
      BufferedReader reader = Files.newBufferedReader(f.path(), f.charset());
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < f.lines(); i++) {
        String lineStr = reader.readLine();
        lineStr = lineStr == null ? "" : lineStr;
        for (int j = 0; j < lineStr.length(); j++) {
          char c = lineStr.charAt(j);
          if (!Character.isWhitespace(c)) {
            sb.append(c);
          }
        }
        if (i > 0) {
          result.append("\n");
        }
        result.append(sb.length() > 0 ? DigestUtils.md5Hex(sb.toString()) : "");
        sb.setLength(0);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Unable to compute line hashes of file " + f, e);
    }

    return result.toString();
  }

  private byte[] computeData(DefaultInputFile inputFile, InputFileMetadata metadata) {
    try {
      return dataFactory.consolidateData(inputFile, metadata);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to read file " + inputFile, e);
    }
  }
}
