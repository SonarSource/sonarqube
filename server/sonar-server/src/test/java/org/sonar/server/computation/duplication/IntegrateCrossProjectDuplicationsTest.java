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

package org.sonar.server.computation.duplication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.FileAttributes;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.ReportComponent.builder;

public class IntegrateCrossProjectDuplicationsTest {

  @Rule
  public LogTester logTester = new LogTester();

  static final String XOO_LANGUAGE = "xoo";

  static final String ORIGIN_FILE_KEY = "ORIGIN_FILE_KEY";
  static final Component ORIGIN_FILE = builder(FILE, 1)
    .setKey(ORIGIN_FILE_KEY)
    .setFileAttributes(new FileAttributes(false, XOO_LANGUAGE))
    .build();

  static final String OTHER_FILE_KEY = "OTHER_FILE_KEY";

  Settings settings = new Settings();

  DuplicationRepository duplicationRepository = mock(DuplicationRepository.class);

  IntegrateCrossProjectDuplications underTest = new IntegrateCrossProjectDuplications(settings, duplicationRepository);

  @Test
  public void add_duplications_from_two_blocks() {
    settings.setProperty("sonar.cpd.xoo.minimumTokens", 10);

    Collection<Block> originBlocks = asList(
      new Block.Builder()
        .setResourceId(ORIGIN_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ec"))
        .setIndexInFile(0)
        .setLines(30, 43)
        .setUnit(0, 5)
        .build(),
      new Block.Builder()
        .setResourceId(ORIGIN_FILE_KEY)
        .setBlockHash(new ByteArray("2b5747f0e4c59124"))
        .setIndexInFile(1)
        .setLines(32, 45)
        .setUnit(5, 20)
        .build()
      );

    Collection<Block> duplicatedBlocks = asList(
      new Block.Builder()
        .setResourceId(OTHER_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ec"))
        .setIndexInFile(0)
        .setLines(40, 53)
        .build(),
      new Block.Builder()
        .setResourceId(OTHER_FILE_KEY)
        .setBlockHash(new ByteArray("2b5747f0e4c59124"))
        .setIndexInFile(1)
        .setLines(42, 55)
        .build());

    underTest.computeCpd(ORIGIN_FILE, originBlocks, duplicatedBlocks);

    verify(duplicationRepository).addDuplication(
      ORIGIN_FILE,
      new TextBlock(30, 45),
      OTHER_FILE_KEY,
      new TextBlock(40, 55)
      );
  }

  @Test
  public void add_duplications_from_a_single_block() {
    settings.setProperty("sonar.cpd.xoo.minimumTokens", 10);

    Collection<Block> originBlocks = singletonList(
      // This block contains 11 tokens -> a duplication will be created
      new Block.Builder()
        .setResourceId(ORIGIN_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ec"))
        .setIndexInFile(0)
        .setLines(30, 45)
        .setUnit(0, 10)
        .build()
      );

    Collection<Block> duplicatedBlocks = singletonList(
      new Block.Builder()
        .setResourceId(OTHER_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ec"))
        .setIndexInFile(0)
        .setLines(40, 55)
        .build()
      );

    underTest.computeCpd(ORIGIN_FILE, originBlocks, duplicatedBlocks);

    verify(duplicationRepository).addDuplication(
      ORIGIN_FILE,
      new TextBlock(30, 45),
      OTHER_FILE_KEY,
      new TextBlock(40, 55)
      );
  }

  @Test
  public void add_no_duplication_from_current_file() {
    settings.setProperty("sonar.cpd.xoo.minimumTokens", 10);

    Collection<Block> originBlocks = asList(
      new Block.Builder()
        .setResourceId(ORIGIN_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ec"))
        .setIndexInFile(0)
        .setLines(30, 45)
        .setUnit(0, 10)
        .build(),
      // Duplication is on the same file
      new Block.Builder()
        .setResourceId(ORIGIN_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ec"))
        .setIndexInFile(0)
        .setLines(46, 60)
        .setUnit(0, 10)
        .build()
      );

    Collection<Block> duplicatedBlocks = singletonList(
      new Block.Builder()
        .setResourceId(OTHER_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ed"))
        .setIndexInFile(0)
        .setLines(40, 55)
        .build()
      );

    underTest.computeCpd(ORIGIN_FILE, originBlocks, duplicatedBlocks);

    verifyNoMoreInteractions(duplicationRepository);
  }

  @Test
  public void add_no_duplication_when_not_enough_tokens() {
    settings.setProperty("sonar.cpd.xoo.minimumTokens", 10);

    Collection<Block> originBlocks = singletonList(
      // This block contains 5 tokens -> not enough to consider it as a duplication
      new Block.Builder()
        .setResourceId(ORIGIN_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ec"))
        .setIndexInFile(0)
        .setLines(30, 45)
        .setUnit(0, 4)
        .build()
      );

    Collection<Block> duplicatedBlocks = singletonList(
      new Block.Builder()
        .setResourceId(OTHER_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ec"))
        .setIndexInFile(0)
        .setLines(40, 55)
        .build()
      );

    underTest.computeCpd(ORIGIN_FILE, originBlocks, duplicatedBlocks);

    verifyNoMoreInteractions(duplicationRepository);
  }

  @Test
  public void add_no_duplication_when_no_duplicated_blocks() {
    settings.setProperty("sonar.cpd.xoo.minimumTokens", 10);

    Collection<Block> originBlocks = singletonList(
      new Block.Builder()
        .setResourceId(ORIGIN_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ec"))
        .setIndexInFile(0)
        .setLines(30, 45)
        .setUnit(0, 10)
        .build()
      );

    underTest.computeCpd(ORIGIN_FILE, originBlocks, Collections.<Block>emptyList());

    verifyNoMoreInteractions(duplicationRepository);
  }

  @Test
  public void add_duplication_for_java_even_when_no_token() {
    Component javaFile = builder(FILE, 1)
      .setKey(ORIGIN_FILE_KEY)
      .setFileAttributes(new FileAttributes(false, "java"))
      .build();

    Collection<Block> originBlocks = singletonList(
      // This block contains 0 token
      new Block.Builder()
        .setResourceId(ORIGIN_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ec"))
        .setIndexInFile(0)
        .setLines(30, 45)
        .setUnit(0, 0)
        .build()
      );

    Collection<Block> duplicatedBlocks = singletonList(
      new Block.Builder()
        .setResourceId(OTHER_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ec"))
        .setIndexInFile(0)
        .setLines(40, 55)
        .build()
      );

    underTest.computeCpd(javaFile, originBlocks, duplicatedBlocks);

    verify(duplicationRepository).addDuplication(
      ORIGIN_FILE,
      new TextBlock(30, 45),
      OTHER_FILE_KEY,
      new TextBlock(40, 55)
      );
  }

  @Test
  public void default_minimum_tokens_is_one_hundred() {
    settings.setProperty("sonar.cpd.xoo.minimumTokens", (Integer) null);

    Collection<Block> originBlocks = singletonList(
      new Block.Builder()
        .setResourceId(ORIGIN_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ec"))
        .setIndexInFile(0)
        .setLines(30, 45)
        .setUnit(0, 100)
        .build()
      );

    Collection<Block> duplicatedBlocks = singletonList(
      new Block.Builder()
        .setResourceId(OTHER_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ec"))
        .setIndexInFile(0)
        .setLines(40, 55)
        .build()
      );

    underTest.computeCpd(ORIGIN_FILE, originBlocks, duplicatedBlocks);

    verify(duplicationRepository).addDuplication(
      ORIGIN_FILE,
      new TextBlock(30, 45),
      OTHER_FILE_KEY,
      new TextBlock(40, 55)
      );
  }

  @Test
  public void do_not_compute_more_than_one_hundred_duplications_when_too_many_duplicated_references() throws Exception {
    Collection<Block> originBlocks = new ArrayList<>();
    Collection<Block> duplicatedBlocks = new ArrayList<>();

    Block.Builder blockBuilder = new Block.Builder()
      .setResourceId(ORIGIN_FILE_KEY)
      .setBlockHash(new ByteArray("a8998353e96320ec"))
      .setIndexInFile(0)
      .setLines(30, 45)
      .setUnit(0, 100);
    originBlocks.add(blockBuilder.build());

    // Generate more than 100 duplications of the same block
    for (int i = 0; i < 110; i++) {
      duplicatedBlocks.add(
        blockBuilder
          .setResourceId(randomAlphanumeric(16))
          .build()
        );
    }

    underTest.computeCpd(ORIGIN_FILE, originBlocks, duplicatedBlocks);

    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly(
      "Too many duplication references on file " + ORIGIN_FILE_KEY + " for block at line 30. Keeping only the first 100 references.");
    verify(duplicationRepository, times(100)).addDuplication(eq(ORIGIN_FILE), any(TextBlock.class), anyString(), any(TextBlock.class));
  }

  @Test
  public void do_not_compute_more_than_one_hundred_duplications_when_too_many_duplications() throws Exception {
    Collection<Block> originBlocks = new ArrayList<>();
    Collection<Block> duplicatedBlocks = new ArrayList<>();

    Block.Builder blockBuilder = new Block.Builder()
      .setIndexInFile(0)
      .setLines(30, 45)
      .setUnit(0, 100);

    // Generate more than 100 duplication on different files
    for (int i = 0; i < 110; i++) {
      String hash = randomAlphanumeric(16);
      originBlocks.add(
        blockBuilder
          .setResourceId(ORIGIN_FILE_KEY)
          .setBlockHash(new ByteArray(hash))
          .build());
      duplicatedBlocks.add(
        blockBuilder
          .setResourceId(randomAlphanumeric(16))
          .setBlockHash(new ByteArray(hash))
          .build()
        );
    }

    underTest.computeCpd(ORIGIN_FILE, originBlocks, duplicatedBlocks);

    verify(duplicationRepository, times(100)).addDuplication(eq(ORIGIN_FILE), any(TextBlock.class), anyString(), any(TextBlock.class));
    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly("Too many duplication groups on file " + ORIGIN_FILE_KEY + ". Keeping only the first 100 groups.");
  }

}
