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
package org.sonar.db.source;

import java.util.Arrays;
import java.util.Date;
import java.util.function.Consumer;
import org.apache.commons.lang.math.RandomUtils;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.protobuf.DbFileSources;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

public class FileSourceTester {

  private final DbTester db;

  public FileSourceTester(DbTester db) {
    this.db = db;
  }

  @SafeVarargs
  public final FileSourceDto insertFileSource(ComponentDto file, Consumer<FileSourceDto>... dtoPopulators) {
    FileSourceDto dto = new FileSourceDto()
      .setProjectUuid(file.projectUuid())
      .setFileUuid(file.uuid())
      .setSrcHash(randomAlphanumeric(50))
      .setDataHash(randomAlphanumeric(50))
      .setLineHashes(randomAlphanumeric(50))
      .setRevision(randomAlphanumeric(100))
      .setSourceData(newRandomData(3).build())
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
        .setScmDate(RandomUtils.nextLong())
        .setSource(randomAlphanumeric(20))
        .setLineHits(RandomUtils.nextInt(4))
        .setConditions(RandomUtils.nextInt(4))
        .setCoveredConditions(RandomUtils.nextInt(4))
        .setHighlighting(randomAlphanumeric(40))
        .setSymbols(randomAlphanumeric(30))
        .addAllDuplication(Arrays.asList(RandomUtils.nextInt(200), RandomUtils.nextInt(200)))
        .build();
    }
    return dataBuilder;
  }
}
