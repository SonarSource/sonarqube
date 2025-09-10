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
package org.sonar.scm.git;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.core.util.ProcessWrapperFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static java.util.Collections.emptyList;
import static org.sonar.api.utils.Preconditions.checkState;

public class NativeGitBlameCommand {
  protected static final String BLAME_COMMAND = "blame";
  protected static final String GIT_DIR_FLAG = "--git-dir";
  protected static final String GIT_DIR_ARGUMENT = "%s/.git";
  protected static final String GIT_DIR_FORCE_FLAG = "-C";

  private static final Logger LOG = LoggerFactory.getLogger(NativeGitBlameCommand.class);
  private static final Pattern EMAIL_PATTERN = Pattern.compile("<(.*?)>");
  private static final String COMMITTER_TIME = "committer-time ";
  private static final String AUTHOR_MAIL = "author-mail ";

  private static final String MINIMUM_REQUIRED_GIT_VERSION = "2.24.0";
  private static final String DEFAULT_GIT_COMMAND = "git";
  private static final String BLAME_LINE_PORCELAIN_FLAG = "--line-porcelain";
  private static final String FILENAME_SEPARATOR_FLAG = "--";
  private static final String IGNORE_WHITESPACES = "-w";

  private static final Pattern whitespaceRegex = Pattern.compile("\\s+");
  private static final Pattern semanticVersionDelimiter = Pattern.compile("\\.");

  private final System2 system;
  private final ProcessWrapperFactory processWrapperFactory;
  private final Consumer<String> stderrConsumer = line -> LOG.debug("[stderr] {}", line);
  private String gitCommand;

  @Autowired
  public NativeGitBlameCommand(System2 system, ProcessWrapperFactory processWrapperFactory) {
    this.system = system;
    this.processWrapperFactory = processWrapperFactory;
  }

  NativeGitBlameCommand(String gitCommand, System2 system, ProcessWrapperFactory processWrapperFactory) {
    this.gitCommand = gitCommand;
    this.system = system;
    this.processWrapperFactory = processWrapperFactory;
  }

  /**
   * This method must be executed before org.sonar.scm.git.GitBlameCommand#blame
   *
   * @return true, if native git is installed
   */
  public boolean checkIfEnabled() {
    try {
      this.gitCommand = locateDefaultGit();
      MutableString stdOut = new MutableString();
      this.processWrapperFactory.create(null, l -> stdOut.string = l, stderrConsumer, gitCommand, "--version").execute();
      return stdOut.string != null && stdOut.string.startsWith("git version") && isCompatibleGitVersion(stdOut.string);
    } catch (Exception e) {
      LOG.debug("Failed to find git native client", e);
      return false;
    }
  }

  private String locateDefaultGit() throws IOException {
    if (this.gitCommand != null) {
      return this.gitCommand;
    }
    // if not set fall back to defaults
    if (system.isOsWindows()) {
      return locateGitOnWindows();
    }
    return DEFAULT_GIT_COMMAND;
  }

  private String locateGitOnWindows() throws IOException {
    // Windows will search current directory in addition to the PATH variable, which is unsecure.
    // To avoid it we use where.exe to find git binary only in PATH.
    LOG.debug("Looking for git command in the PATH using where.exe (Windows)");
    List<String> whereCommandResult = new LinkedList<>();
    this.processWrapperFactory.create(null, whereCommandResult::add, stderrConsumer, "C:\\Windows\\System32\\where.exe", "$PATH:git.exe")
      .execute();

    if (!whereCommandResult.isEmpty()) {
      String out = whereCommandResult.get(0).trim();
      LOG.debug("Found git.exe at {}", out);
      return out;
    }
    throw new IllegalStateException("git.exe not found in PATH. PATH value was: " + system.property("PATH"));
  }

  public List<BlameLine> blame(Path baseDir, String fileName) throws IOException {
    BlameOutputProcessor outputProcessor = new BlameOutputProcessor();
    var processWrapper = this.processWrapperFactory.create(
      baseDir,
      outputProcessor::process,
      stderrConsumer,
      gitCommand,
      GIT_DIR_FLAG, String.format(GIT_DIR_ARGUMENT, baseDir), GIT_DIR_FORCE_FLAG, baseDir.toString(),
      BLAME_COMMAND,
      BLAME_LINE_PORCELAIN_FLAG, IGNORE_WHITESPACES, FILENAME_SEPARATOR_FLAG, fileName);
    outputProcessor.setProcessWrapper(processWrapper);
    processWrapper.execute();
    if (outputProcessor.hasEncounteredUncommittedLine()) {
      LOG.debug("Unable to blame file '{}' - it has uncommitted changes", fileName);
      return emptyList();
    }
    return outputProcessor.getBlameLines();
  }

  public static class BlameOutputProcessor {
    private final List<BlameLine> blameLines = new LinkedList<>();
    private String sha1 = null;
    private String committerTime = null;
    private String authorMail = null;
    private ProcessWrapperFactory.ProcessWrapper processWrapper = null;
    private volatile boolean encounteredUncommittedLine = false;

    public List<BlameLine> getBlameLines() {
      return blameLines;
    }

    public void process(String line) {
      line = sanitizeLine(line);
      if (sha1 == null) {
        sha1 = line.split(" ")[0];
      } else if (line.startsWith("\t")) {
        saveEntry();
      } else if (line.startsWith(COMMITTER_TIME)) {
        committerTime = line.substring(COMMITTER_TIME.length());
      } else if (line.startsWith(AUTHOR_MAIL)) {
        Matcher matcher = EMAIL_PATTERN.matcher(line);
        if (matcher.find(AUTHOR_MAIL.length())) {
          authorMail = matcher.group(1);
        }
        if (authorMail.equals("not.committed.yet")) {
          encounteredUncommittedLine = true;
          processWrapper.destroy();
        }
      }
    }

    /**
     * Removes null characters from the line.
     * It may happen that the blame command output contains null characters, especially on Windows environments.
     * This can lead to issues when persisting the revision in the database.
     */
    private static String sanitizeLine(String line) {
      if (line.contains("\u0000")) {
        LOG.debug("Encountered blame output line with null character: '{}'", line);
        return line.replace("\u0000", "");
      }
      return line;
    }

    public boolean hasEncounteredUncommittedLine() {
      return encounteredUncommittedLine;
    }

    private void saveEntry() {
      checkState(authorMail != null, "Did not find an author email for an entry");
      checkState(committerTime != null, "Did not find a committer time for an entry");
      checkState(sha1 != null, "Did not find a commit sha1 for an entry");
      try {
        blameLines.add(new BlameLine()
          .revision(sha1)
          .author(authorMail)
          .date(Date.from(Instant.ofEpochSecond(Long.parseLong(committerTime)))));
      } catch (NumberFormatException e) {
        throw new IllegalStateException("Invalid committer time found: " + committerTime);
      }
      authorMail = null;
      sha1 = null;
      committerTime = null;
    }

    public void setProcessWrapper(ProcessWrapperFactory.ProcessWrapper processWrapper) {
      this.processWrapper = processWrapper;
    }
  }

  private static boolean isCompatibleGitVersion(String gitVersionCommandOutput) {
    // Due to the danger of argument injection on git blame the use of `--end-of-options` flag is necessary
    // The flag is available only on git versions >= 2.24.0
    String gitVersion = whitespaceRegex
      .splitAsStream(gitVersionCommandOutput)
      .skip(2)
      .findFirst()
      .orElse("");

    String formattedGitVersion = formatGitSemanticVersion(gitVersion);
    return Version.parse(formattedGitVersion).isGreaterThanOrEqual(Version.parse(MINIMUM_REQUIRED_GIT_VERSION));
  }

  private static String formatGitSemanticVersion(String version) {
    return semanticVersionDelimiter
      .splitAsStream(version)
      .takeWhile(NumberUtils::isCreatable)
      .collect(Collectors.joining("."));
  }

  private static class MutableString {
    String string;
  }

}
