/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scm.git;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.scm.BlameCommand;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.System2;
import org.sonar.scm.git.strategy.DefaultBlameStrategy.BlameAlgorithmEnum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class CompositeBlameCommandIT {

  private final AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);

  private final BlameCommand.BlameInput input = mock(BlameCommand.BlameInput.class);
  private final JGitBlameCommand jGitBlameCommand = new JGitBlameCommand();

  private final ProcessWrapperFactory processWrapperFactory = new ProcessWrapperFactory();
  private final NativeGitBlameCommand nativeGitBlameCommand = new NativeGitBlameCommand(System2.INSTANCE, processWrapperFactory);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  @UseDataProvider("namesOfTheTestRepositoriesWithBlameAlgorithm")
  public void testThatBlameAlgorithmOutputsTheSameDataAsGitNativeBlame(String folder, BlameAlgorithmEnum blameAlgorithm) throws Exception {
    CompositeBlameCommand underTest = new CompositeBlameCommand(analysisWarnings, new PathResolver(), jGitBlameCommand, nativeGitBlameCommand, (p, f) -> blameAlgorithm);

    TestBlameOutput output = new TestBlameOutput();
    File gitFolder = unzipGitRepository(folder);

    setUpBlameInputWithFile(gitFolder.toPath());

    underTest.blame(input, output);

    assertBlameMatchesExpectedBlame(output.blame, gitFolder);
  }

  @DataProvider
  public static Object[][] namesOfTheTestRepositoriesWithBlameAlgorithm() {
    List<String> testCases = List.of("one-file-one-commit",
      "one-file-two-commits",
      "two-files-one-commit",
      "merge-commits",
      "5lines-5commits",
      "5files-5commits",
      "two-files-moved-around-with-conflicts",
      "one-file-renamed-many-times",
      "one-file-many-merges-and-renames",
      "two-merge-commits",
      "dummy-git",
      "dummy-git-few-comitters"
      );

    List<BlameAlgorithmEnum> blameStrategies = Arrays.stream(BlameAlgorithmEnum.values()).collect(Collectors.toList());
    return testCases.stream()
      .flatMap(t -> blameStrategies.stream().map(b -> new Object[]{t, b}))
      .toArray(Object[][]::new);
  }


  private void assertBlameMatchesExpectedBlame(Map<InputFile, List<BlameLine>> blame, File gitFolder) throws Exception {
    Map<Path, List<BlameLine>> expectedBlame = readExpectedBlame(gitFolder.getName());

    assertThat(blame.entrySet())
      .as("Blamed files: " + blame.keySet() + ". Expected blamed files " + expectedBlame.keySet())
      .hasSize(expectedBlame.size());

    for (Map.Entry<InputFile, List<BlameLine>> actualBlame : blame.entrySet()) {
      Path relativeFilePath = gitFolder.toPath().relativize(actualBlame.getKey().path());
      assertThat(actualBlame.getValue())
        .as("A difference is found in file " + relativeFilePath)
        .isEqualTo(expectedBlame.get(relativeFilePath));
    }
  }

  // --- helper methods --- //

  private Map<Path, List<BlameLine>> readExpectedBlame(String expectedBlameFolder) throws Exception {
    Path expectedBlameFiles = new File(Utils.class.getResource("expected-blame/" + expectedBlameFolder).toURI()).toPath();
    Map<Path, List<BlameLine>> expectedBlame = new HashMap<>();

    List<Path> filesInExpectedBlameFolder = Files.walk(expectedBlameFiles).filter(Files::isRegularFile).collect(Collectors.toList());
    for (Path expectedFileBlamePath : filesInExpectedBlameFolder) {
      List<BlameLine> blameLines = new ArrayList<>();
      List<String> expectedBlameStrings = Files.readAllLines(expectedFileBlamePath);
      for (String line : expectedBlameStrings) {
        String revision = line.substring(0, 40);

        int beginningEmail = line.indexOf("<") + 1, endEmail = line.indexOf(">");
        String email = line.substring(beginningEmail, endEmail);

        int beginningDate = line.indexOf("2", endEmail), dateLength = 25;
        String sDate = line.substring(beginningDate, beginningDate + dateLength);
        Date parsedDate = new Date(OffsetDateTime.parse(sDate).toInstant().toEpochMilli());

        BlameLine blameLine = new BlameLine()
          .revision(revision)
          .author(email)
          .date(parsedDate);

        blameLines.add(blameLine);
      }
      expectedBlame.put(expectedBlameFiles.relativize(expectedFileBlamePath), blameLines);
    }
    return expectedBlame;
  }

  private File unzipGitRepository(String repositoryName) throws IOException {
    File gitFolderForEachTest = temp.newFolder().toPath().toRealPath(LinkOption.NOFOLLOW_LINKS).toFile();
    Utils.javaUnzip(repositoryName + ".zip", gitFolderForEachTest);
    return gitFolderForEachTest.toPath().resolve(repositoryName).toFile();
  }

  private static class TestBlameOutput implements BlameCommand.BlameOutput {
    private final Map<InputFile, List<BlameLine>> blame = new ConcurrentHashMap<>();

    @Override
    public void blameResult(InputFile inputFile, List<BlameLine> list) {
      blame.put(inputFile, list);
    }
  }

  private void setUpBlameInputWithFile(Path baseDir) throws IOException {
    DefaultFileSystem fs = new DefaultFileSystem(baseDir);
    when(input.fileSystem()).thenReturn(fs);

    try (Stream<Path> stream = Files.walk(baseDir)) {
      List<InputFile> inputFiles = stream.filter(Files::isRegularFile)
        .map(f -> new TestInputFileBuilder("foo", baseDir.toFile(), f.toFile()).build())
        .filter(f -> !f.toString().startsWith(".git") && !f.toString().endsWith(".class"))
        .collect(Collectors.toList());
      when(input.filesToBlame()).thenReturn(inputFiles);
    }
  }
}
