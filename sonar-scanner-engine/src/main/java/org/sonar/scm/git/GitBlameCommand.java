/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.scm.BlameLine;

import static java.util.Objects.requireNonNull;

public class GitBlameCommand {
  private static final String AUTHOR = "author";
  private static final String COMMIT = "commit";
  private static final String TIMESTAMP = "timestamp";
  private static final String TIMEZONE = "timezone";
  private static final String LINE = "line";

  private static final String GIT_COMMAND = "git";
  private static final String BLAME_COMMAND = "blame";
  private static final String BLAME_LONG_FLAG = "-l";
  private static final String BLAME_SHOW_EMAIL_FLAG = "--show-email";
  private static final String BLAME_TIMESTAMP_FLAG = "-t";

  public static List<BlameLine> executeCommand(Path directory, String... command) throws IOException, InterruptedException {
    requireNonNull(directory, "directory");

    if (!Files.exists(directory)) {
      throw new RuntimeException("Directory does not exist, unable to run git operations:'" + directory + "'");
    }

    ProcessBuilder pb = new ProcessBuilder()
      .command(command)
      .directory(directory.toFile());

    Process p = pb.start();

    List<String> commandOutput = new ArrayList<>();
    InputStream processStdOutput = p.getInputStream();

    try (BufferedReader br = new BufferedReader(new InputStreamReader(processStdOutput))) {
        String outputLine;

        while ((outputLine = br.readLine()) != null) {
          commandOutput.add(outputLine);
        }

        int exit = p.waitFor();

        if (exit != 0) {
          throw new AssertionError(String.format("Command execution exited with code: %d", exit));
        }

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      p.destroy();
    }

    return commandOutput
      .stream()
      .map(GitBlameCommand::parseBlameLine)
      .collect(Collectors.toList());
  }

  private static Map<String, String> getBlameAuthoringData(String blameLine) {
    String[] blameLineFormatted = blameLine.trim().split("\\s+", 2);

    String commit = blameLineFormatted[0];

    if (commit.length() != 40) {
      throw new IllegalStateException(String.format("Failed to fetch correct commit hash, must be of length 40: %s", commit));
    }

    String authoringData = StringUtils.substringBetween(blameLineFormatted[1], "(", ")");
    String[] authoringDataFormatted = authoringData.trim().split("\\s+", 4);

    String author = StringUtils.substringBetween(authoringDataFormatted[0], "<", ">");
    String timestamp = authoringDataFormatted[1];
    String timezone = authoringDataFormatted[2];
    String line = authoringDataFormatted[3];

    Map<String, String> blameData = new HashMap<>();

    blameData.put(COMMIT, commit);
    blameData.put(AUTHOR, author);
    blameData.put(TIMESTAMP, timestamp);
    blameData.put(TIMEZONE, timezone);
    blameData.put(LINE, line);

    return blameData;
  }

  private static BlameLine parseBlameLine(String blameLine) {
    Map<String, String> blameData = getBlameAuthoringData(blameLine);

    return new BlameLine()
      .date(new Date(Long.parseLong(blameData.get(TIMESTAMP)))) // should also take timezone into consideration
      .revision(blameData.get(COMMIT))
      .author(blameData.get(AUTHOR));
  }

  public static void gitInit(Path directory) throws IOException, InterruptedException {
    executeCommand(directory, "git", "init");
  }

  public static void gitStage(Path directory) throws IOException, InterruptedException {
    executeCommand(directory, "git", "add", "-A");
  }

  public static void gitCommit(Path directory, String message) throws IOException, InterruptedException {
    executeCommand(directory, GIT_COMMAND, COMMIT, "-m", message);
  }

  public static void gitClone(Path directory, String originUrl) throws IOException, InterruptedException {
    executeCommand(directory.getParent(), "git", "clone", originUrl, directory.getFileName().toString());
  }

  public static List<BlameLine> gitBlame(Path directory, String fileName) throws IOException, InterruptedException {
    return executeCommand(directory, GIT_COMMAND, BLAME_COMMAND, BLAME_LONG_FLAG, BLAME_SHOW_EMAIL_FLAG, BLAME_TIMESTAMP_FLAG, fileName);
  }

  private static class StreamGobbler extends Thread {
    private final InputStream is;
    private final String type;

    private StreamGobbler(InputStream is, String type) {
      this.is = is;
      this.type = type;
    }

    @Override
    public void run() {
      try (BufferedReader br = new BufferedReader(new InputStreamReader(is));) {
        List<String> commandOutput = new ArrayList<>();
        String outputLine;

        while ((outputLine = br.readLine()) != null) {
          commandOutput.add(outputLine);
          System.out.println(type + "> " + outputLine);
        }
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }

}
