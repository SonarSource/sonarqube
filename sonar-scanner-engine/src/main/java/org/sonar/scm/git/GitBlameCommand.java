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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.springframework.beans.factory.annotation.Autowired;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static org.sonar.api.utils.Preconditions.checkState;

public class GitBlameCommand {
  private static final Logger LOG = Loggers.get(GitBlameCommand.class);
  private static final Pattern EMAIL_PATTERN = Pattern.compile("<(\\S*?)>");
  private static final String COMITTER_TIME = "committer-time ";
  private static final String COMITTER_MAIL = "committer-mail ";

  private static final String GIT_COMMAND = "git";
  private static final String BLAME_COMMAND = "blame";
  private static final String BLAME_LINE_PORCELAIN_FLAG = "--line-porcelain";
  private static final String IGNORE_WHITESPACES = "-w";

  private final String gitCommand;

  @Autowired
  public GitBlameCommand() {
    this(GIT_COMMAND);
  }

  public GitBlameCommand(String gitCommand) {
    this.gitCommand = gitCommand;
  }

  public boolean isEnabled() {
    try {
      MutableString stdOut = new MutableString();
      executeCommand(null, l -> stdOut.string = l, gitCommand, "--version");
      return stdOut.string != null && stdOut.string.startsWith("git version");
    } catch (Exception e) {
      LOG.debug("Failed to find git native client", e);
      return false;
    }
  }

  public List<BlameLine> blame(Path baseDir, String fileName) throws Exception {
    BlameOutputProcessor outputProcessor = new BlameOutputProcessor();
    try {
      executeCommand(baseDir, outputProcessor::process, gitCommand, BLAME_COMMAND, BLAME_LINE_PORCELAIN_FLAG, IGNORE_WHITESPACES, fileName);
    } catch (UncommittedLineException e) {
      LOG.debug("Unable to blame file '{}' - it has uncommitted changes", fileName);
      return emptyList();
    }
    return outputProcessor.getBlameLines();
  }

  private static void executeCommand(@Nullable Path baseDir, Consumer<String> stdOutLineConsumer, String... command) throws Exception {
    ProcessBuilder pb = new ProcessBuilder()
      .command(command)
      .directory(baseDir != null ? baseDir.toFile() : null);

    Process p = pb.start();
    try {
      InputStream processStdOutput = p.getInputStream();
      // don't use BufferedReader#readLine because it will also parse CR, which may be part of the actual source code line
      try (Scanner scanner = new Scanner(new InputStreamReader(processStdOutput, UTF_8))) {
        scanner.useDelimiter("\n");
        while (scanner.hasNext()) {
          stdOutLineConsumer.accept(scanner.next());
        }
      }

      int exit = p.waitFor();
      if (exit != 0) {
        throw new IllegalStateException(String.format("Command execution exited with code: %d", exit));
      }
    } finally {
      p.destroy();
    }
  }

  private static class BlameOutputProcessor {
    private final List<BlameLine> blameLines = new LinkedList<>();
    private String sha1 = null;
    private String committerTime = null;
    private String committerMail = null;

    public List<BlameLine> getBlameLines() {
      return blameLines;
    }

    public void process(String line) {
      if (sha1 == null) {
        sha1 = line.split(" ")[0];
      } else if (line.startsWith("\t")) {
        saveEntry();
      } else if (line.startsWith(COMITTER_TIME)) {
        committerTime = line.substring(COMITTER_TIME.length());
      } else if (line.startsWith(COMITTER_MAIL)) {
        Matcher matcher = EMAIL_PATTERN.matcher(line);
        if (!matcher.find(COMITTER_MAIL.length()) || matcher.groupCount() != 1) {
          throw new IllegalStateException("Couldn't parse committer email from: " + line);
        }
        committerMail = matcher.group(1);
        if (committerMail.equals("not.committed.yet")) {
          throw new UncommittedLineException();
        }
      }
    }

    private void saveEntry() {
      checkState(committerMail != null, "Did not find a committer email for an entry");
      checkState(committerTime != null, "Did not find a committer time for an entry");
      checkState(sha1 != null, "Did not find a commit sha1 for an entry");
      try {
        blameLines.add(new BlameLine()
          .revision(sha1)
          .author(committerMail)
          .date(Date.from(Instant.ofEpochSecond(Long.parseLong(committerTime)))));
      } catch (NumberFormatException e) {
        throw new IllegalStateException("Invalid committer time found: " + committerTime);
      }
      committerMail = null;
      sha1 = null;
      committerTime = null;
    }
  }

  private static class MutableString {
    String string;
  }

  private static class UncommittedLineException extends RuntimeException {

  }
}
