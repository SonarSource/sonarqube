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
package org.sonar.db.source;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.protobuf.DbFileSources;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

public class FileSourceTester {

  private static final Random RANDOM = new SecureRandom();

  private final DbTester db;

  public FileSourceTester(DbTester db) {
    this.db = db;
  }

  @SafeVarargs
  public final FileSourceDto insertFileSource(ComponentDto file, Consumer<FileSourceDto>... dtoPopulators) {
    FileSourceDto dto = new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid(file.branchUuid())
      .setFileUuid(file.uuid())
      .setSrcHash(randomAlphanumeric(50))
      .setDataHash(randomAlphanumeric(50))
      .setLineHashes(IntStream.range(0, RANDOM.nextInt(21)).mapToObj(String::valueOf).toList())
      .setRevision(randomAlphanumeric(100))
      .setSourceData(newRandomData(3).build())
      .setCreatedAt(new Date().getTime())
      .setUpdatedAt(new Date().getTime());
    Arrays.stream(dtoPopulators).forEach(c -> c.accept(dto));
    db.getDbClient().fileSourceDao().insert(db.getSession(), dto);
    db.commit();
    dto.setUuid(db.getDbClient().fileSourceDao().selectByFileUuid(db.getSession(), dto.getFileUuid()).getUuid());
    return dto;
  }

  @SafeVarargs
  public final FileSourceDto insertFileSource(ComponentDto file, int numLines, Consumer<FileSourceDto>... dtoPopulators) {
    FileSourceDto dto = new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid(file.branchUuid())
      .setFileUuid(file.uuid())
      .setSrcHash(randomAlphanumeric(50))
      .setDataHash(randomAlphanumeric(50))
      .setLineHashes(IntStream.range(0, numLines).mapToObj(String::valueOf).toList())
      .setRevision(randomAlphanumeric(100))
      .setSourceData(newRandomData(numLines).build())
      .setCreatedAt(new Date().getTime())
      .setUpdatedAt(new Date().getTime());
    Arrays.stream(dtoPopulators).forEach(c -> c.accept(dto));
    db.getDbClient().fileSourceDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

  private static DbFileSources.Data.Builder newRandomData(int numberOfLines) {
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    for (int i = 1; i <= numberOfLines; i++) {
      dataBuilder.addLinesBuilder()
        .setLine(i)
        .setScmRevision(randomAlphanumeric(15))
        .setScmAuthor(randomAlphanumeric(10))
        .setScmDate(RANDOM.nextLong(Long.MAX_VALUE))
        .setSource(randomAlphanumeric(20))
        .setLineHits(RANDOM.nextInt(4))
        .setConditions(RANDOM.nextInt(4))
        .setCoveredConditions(RANDOM.nextInt(4))
        .addAllDuplication(Arrays.asList(RANDOM.nextInt(200), RANDOM.nextInt(200)))
        .build();
    }
    return dataBuilder;
  }
}
