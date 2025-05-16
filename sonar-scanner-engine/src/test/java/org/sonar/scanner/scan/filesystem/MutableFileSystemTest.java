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
package org.sonar.scanner.scan.filesystem;

import java.util.Locale;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class MutableFileSystemTest {

  private static final String LANGUAGE = "php";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private MutableFileSystem underTest;

  @Before
  public void prepare() throws Exception {
    underTest = new MutableFileSystem(temp.newFolder().toPath());
  }

  @Test
  public void restriction_and_hidden_file_should_be_disabled_on_default() {
    assertThat(underTest.restrictToChangedFiles).isFalse();
    assertThat(underTest.allowHiddenFileAnalysis).isFalse();
  }

  @Test
  public void return_all_non_hidden_files_when_not_restricted_and_disabled() {
    assertThat(underTest.inputFiles(underTest.predicates().all())).isEmpty();
    addFilesWithAllStatus();
    underTest.setRestrictToChangedFiles(false);

    assertThat(underTest.inputFiles(underTest.predicates().all())).hasSize(3);
    assertThat(underTest.inputFile(underTest.predicates().hasFilename(generateFilename(InputFile.Status.ADDED)))).isNotNull();
    assertThat(underTest.inputFile(underTest.predicates().hasFilename(generateFilename(InputFile.Status.SAME)))).isNotNull();
    assertThat(underTest.inputFile(underTest.predicates().hasFilename(generateFilename(InputFile.Status.CHANGED)))).isNotNull();
  }

  @Test
  public void return_only_changed_files_when_restricted() {
    assertThat(underTest.inputFiles(underTest.predicates().all())).isEmpty();
    addFilesWithAllStatus();
    underTest.setRestrictToChangedFiles(true);

    assertThat(underTest.inputFiles(underTest.predicates().all())).hasSize(2);
    assertThat(underTest.inputFile(underTest.predicates().hasFilename(generateFilename(InputFile.Status.ADDED)))).isNotNull();
    assertThat(underTest.inputFile(underTest.predicates().hasFilename(generateFilename(InputFile.Status.SAME)))).isNull();
    assertThat(underTest.inputFile(underTest.predicates().hasFilename(generateFilename(InputFile.Status.CHANGED)))).isNotNull();
  }

  @Test
  public void return_all_files_when_allowing_hidden_files_analysis() {
    assertThat(underTest.inputFiles(underTest.predicates().all())).isEmpty();
    addFilesWithVisibility();
    underTest.setAllowHiddenFileAnalysis(true);

    assertThat(underTest.inputFiles(underTest.predicates().all())).hasSize(2);
    assertThat(underTest.inputFile(underTest.predicates().hasFilename(generateFilename(true)))).isNotNull();
    assertThat(underTest.inputFile(underTest.predicates().hasFilename(generateFilename(false)))).isNotNull();
  }

  @Test
  public void return_only_non_hidden_files_when_not_allowing_hidden_files_analysis() {
    assertThat(underTest.inputFiles(underTest.predicates().all())).isEmpty();
    addFilesWithVisibility();
    underTest.setAllowHiddenFileAnalysis(false);

    assertThat(underTest.inputFiles(underTest.predicates().all())).hasSize(1);
    assertThat(underTest.inputFile(underTest.predicates().hasFilename(generateFilename(true)))).isNull();
    assertThat(underTest.inputFile(underTest.predicates().hasFilename(generateFilename(false)))).isNotNull();
  }

  private void addFilesWithVisibility() {
    addFile(true);
    addFile(false);
  }

  private void addFilesWithAllStatus() {
    addFile(InputFile.Status.ADDED);
    addFile(InputFile.Status.CHANGED);
    addFile(InputFile.Status.SAME);
  }

  private void addFile(InputFile.Status status) {
    addFile(status, false);
  }

  private void addFile(boolean hidden) {
    addFile(InputFile.Status.SAME, hidden);
  }

  private void addFile(InputFile.Status status, boolean hidden) {
    underTest.add(new TestInputFileBuilder("foo", String.format("src/%s", generateFilename(status, hidden)))
      .setLanguage(LANGUAGE).setStatus(status).setHidden(hidden).build());
  }

  private String generateFilename(boolean hidden) {
    return generateFilename(InputFile.Status.SAME, hidden);
  }

  private String generateFilename(InputFile.Status status) {
    return generateFilename(status, false);
  }

  private String generateFilename(InputFile.Status status, boolean hidden) {
    return String.format("%s.%s.%s", status.name().toLowerCase(Locale.ROOT), hidden, LANGUAGE);
  }

}
