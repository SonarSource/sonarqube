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
package org.sonar.ce.task.projectanalysis.duplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.task.log.CeTaskMessages;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;

import static com.google.common.base.Strings.padStart;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

public class IntegrateCrossProjectDuplicationsTest {
  private static final String XOO_LANGUAGE = "xoo";
  private static final String ORIGIN_FILE_KEY = "ORIGIN_FILE_KEY";
  private static final Component ORIGIN_FILE = builder(FILE, 1)
    .setKey(ORIGIN_FILE_KEY)
    .setFileAttributes(new FileAttributes(false, XOO_LANGUAGE, 1))
    .build();
  private static final String OTHER_FILE_KEY = "OTHER_FILE_KEY";

  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public DuplicationRepositoryRule duplicationRepository = DuplicationRepositoryRule.create();

  private TestSystem2 system = new TestSystem2();
  private MapSettings settings = new MapSettings();
  private CeTaskMessages ceTaskMessages = mock(CeTaskMessages.class);
  private IntegrateCrossProjectDuplications underTest = new IntegrateCrossProjectDuplications(settings.asConfig(), duplicationRepository, ceTaskMessages, system);

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
        .build());

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

    assertThat(duplicationRepository.getDuplications(ORIGIN_FILE))
      .containsExactly(
        crossProjectDuplication(new TextBlock(30, 45), OTHER_FILE_KEY, new TextBlock(40, 55)));
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
        .build());

    Collection<Block> duplicatedBlocks = singletonList(
      new Block.Builder()
        .setResourceId(OTHER_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ec"))
        .setIndexInFile(0)
        .setLines(40, 55)
        .build());

    underTest.computeCpd(ORIGIN_FILE, originBlocks, duplicatedBlocks);

    assertThat(duplicationRepository.getDuplications(ORIGIN_FILE))
      .containsExactly(
        crossProjectDuplication(new TextBlock(30, 45), OTHER_FILE_KEY, new TextBlock(40, 55)));
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
        .build());

    Collection<Block> duplicatedBlocks = singletonList(
      new Block.Builder()
        .setResourceId(OTHER_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ed"))
        .setIndexInFile(0)
        .setLines(40, 55)
        .build());

    underTest.computeCpd(ORIGIN_FILE, originBlocks, duplicatedBlocks);

    assertNoDuplicationAdded(ORIGIN_FILE);
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
        .build());

    Collection<Block> duplicatedBlocks = singletonList(
      new Block.Builder()
        .setResourceId(OTHER_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ec"))
        .setIndexInFile(0)
        .setLines(40, 55)
        .build());

    underTest.computeCpd(ORIGIN_FILE, originBlocks, duplicatedBlocks);

    assertNoDuplicationAdded(ORIGIN_FILE);
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
        .build());

    underTest.computeCpd(ORIGIN_FILE, originBlocks, Collections.emptyList());

    assertNoDuplicationAdded(ORIGIN_FILE);
  }

  @Test
  public void add_duplication_for_java_even_when_no_token() {
    Component javaFile = builder(FILE, 1)
      .setKey(ORIGIN_FILE_KEY)
      .setFileAttributes(new FileAttributes(false, "java", 10))
      .build();

    Collection<Block> originBlocks = singletonList(
      // This block contains 0 token
      new Block.Builder()
        .setResourceId(ORIGIN_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ec"))
        .setIndexInFile(0)
        .setLines(30, 45)
        .setUnit(0, 0)
        .build());

    Collection<Block> duplicatedBlocks = singletonList(
      new Block.Builder()
        .setResourceId(OTHER_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ec"))
        .setIndexInFile(0)
        .setLines(40, 55)
        .build());

    underTest.computeCpd(javaFile, originBlocks, duplicatedBlocks);

    assertThat(duplicationRepository.getDuplications(ORIGIN_FILE))
      .containsExactly(
        crossProjectDuplication(new TextBlock(30, 45), OTHER_FILE_KEY, new TextBlock(40, 55)));
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
        .build());

    Collection<Block> duplicatedBlocks = singletonList(
      new Block.Builder()
        .setResourceId(OTHER_FILE_KEY)
        .setBlockHash(new ByteArray("a8998353e96320ec"))
        .setIndexInFile(0)
        .setLines(40, 55)
        .build());

    underTest.computeCpd(ORIGIN_FILE, originBlocks, duplicatedBlocks);

    assertThat(duplicationRepository.getDuplications(ORIGIN_FILE))
      .containsExactly(
        crossProjectDuplication(new TextBlock(30, 45), OTHER_FILE_KEY, new TextBlock(40, 55)));
  }

  @Test
  public void do_not_compute_more_than_one_hundred_duplications_when_too_many_duplicated_references() {
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
          .build());
    }

    underTest.computeCpd(ORIGIN_FILE, originBlocks, duplicatedBlocks);

    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly(
      "Too many duplication references on file " + ORIGIN_FILE_KEY + " for block at line 30. Keeping only the first 100 references.");
    Iterable<Duplication> duplications = duplicationRepository.getDuplications(ORIGIN_FILE);
    assertThat(duplications).hasSize(1);
    assertThat(duplications.iterator().next().getDuplicates()).hasSize(100);
  }

  @Test
  public void do_not_compute_more_than_one_hundred_duplications_when_too_many_duplications() {
    Collection<Block> originBlocks = new ArrayList<>();
    Collection<Block> duplicatedBlocks = new ArrayList<>();

    Block.Builder blockBuilder = new Block.Builder()
      .setIndexInFile(0)
      .setLines(30, 45)
      .setUnit(0, 100);

    // Generate more than 100 duplication on different files
    for (int i = 0; i < 110; i++) {
      String hash = padStart("hash" + i, 16, 'a');
      originBlocks.add(
        blockBuilder
          .setResourceId(ORIGIN_FILE_KEY)
          .setBlockHash(new ByteArray(hash))
          .build());
      duplicatedBlocks.add(
        blockBuilder
          .setResourceId("resource" + i)
          .setBlockHash(new ByteArray(hash))
          .build());
    }

    underTest.computeCpd(ORIGIN_FILE, originBlocks, duplicatedBlocks);

    assertThat(duplicationRepository.getDuplications(ORIGIN_FILE)).hasSize(100);
    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly("Too many duplication groups on file " + ORIGIN_FILE_KEY + ". Keeping only the first 100 groups.");
  }

  @Test
  public void log_warning_if_this_deprecated_feature_is_enabled() {
    settings.setProperty("sonar.cpd.cross_project", "true");
    system.setNow(1000L);

    new IntegrateCrossProjectDuplications(settings.asConfig(), duplicationRepository, ceTaskMessages, system);

    assertThat(logTester.logs()).containsExactly("This analysis uses the deprecated cross-project duplication feature.");
    verify(ceTaskMessages).add(new CeTaskMessages.Message("This project uses the deprecated cross-project duplication feature.", 1000L));
  }

  private static Duplication crossProjectDuplication(TextBlock original, String otherFileKey, TextBlock duplicate) {
    return new Duplication(original, Arrays.asList(new CrossProjectDuplicate(otherFileKey, duplicate)));
  }

  private void assertNoDuplicationAdded(Component file) {
    assertThat(duplicationRepository.getDuplications(file)).isEmpty();
  }

}
