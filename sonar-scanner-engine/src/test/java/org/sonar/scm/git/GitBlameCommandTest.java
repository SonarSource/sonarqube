package org.sonar.scm.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Test;
import org.sonar.api.batch.scm.BlameLine;

import static org.sonar.scm.git.GitBlameCommand.gitBlame;
import static org.sonar.scm.git.GitBlameCommand.gitClone;
import static org.sonar.scm.git.GitBlameCommand.gitCommit;
import static org.sonar.scm.git.GitBlameCommand.gitInit;
import static org.sonar.scm.git.GitBlameCommand.gitStage;
import static org.assertj.core.api.Assertions.assertThat;

public class GitBlameCommandTest {
  private static final String ORIGIN_URL = "https://github.com/klaussinani/taskbook.git";

  @Test
  public void testBlame() throws IOException, InterruptedException {
    String tmpDirectory = Files.createTempDirectory("tmpDirectory").toFile().getAbsolutePath();
    Path directory = Paths.get(tmpDirectory);
    gitClone(directory, ORIGIN_URL);
    List<BlameLine> blameOutput = gitBlame(directory, "readme.md");
    assertThat(blameOutput.size()).isEqualTo(378);
  }

  @Test
  public void initAndAddFile() throws IOException, InterruptedException {
    String tmpDirectory = Files.createTempDirectory("tmpDirectory").toFile().getAbsolutePath();
    Path directory = Paths.get(tmpDirectory);
    Files.createDirectories(directory);
    gitInit(directory);
    Files.write(directory.resolve("bar.c"), new byte[0]);
    gitStage(directory);
    gitCommit(directory, "Add bar.c");
  }

  @Test
  public void cloneAndAddFile() throws IOException, InterruptedException {
    String tmpDirectory = Files.createTempDirectory("tmpDirectory").toFile().getAbsolutePath();
    Path directory = Paths.get(tmpDirectory);
    gitClone(directory, ORIGIN_URL);
    Files.write(directory.resolve("bar.c"), new byte[0]);
    gitStage(directory);
    gitCommit(directory, "Add bar.c");
    // gitPush(directory); // don't push
  }
}