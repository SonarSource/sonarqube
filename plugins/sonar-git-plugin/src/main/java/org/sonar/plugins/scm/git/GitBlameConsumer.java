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
package org.sonar.plugins.scm.git;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.utils.command.StreamConsumer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitBlameConsumer implements StreamConsumer {

  private static final String GIT_COMMITTER_PREFIX = "committer";
  private static final String GIT_COMMITTER_TIME = GIT_COMMITTER_PREFIX + "-time ";
  private static final String GIT_AUTHOR_EMAIL = "author-mail ";
  private static final String GIT_COMMITTER_EMAIL = GIT_COMMITTER_PREFIX + "-mail ";
  private static final String OPENING_EMAIL_FIELD = "<";
  private static final String CLOSING_EMAIL_FIELD = ">";

  private List<BlameLine> lines = new ArrayList<BlameLine>();

  /**
   * Since the porcelain format only contains the commit information
   * the first time a specific sha-1 commit appears, we need to store
   * this information somwehere.
   * <p/>
   * key: the sha-1 of the commit
   * value: the {@link BlameLine} containing the full committer/author info
   */
  private Map<String, BlameLine> commitInfo = new HashMap<String, BlameLine>();

  private boolean expectRevisionLine = true;

  private String revision = null;
  private String author = null;
  private String committer = null;
  private Date time = null;
  private final String filename;

  public GitBlameConsumer(String filename) {
    this.filename = filename;
  }

  public void consumeLine(String line) {
    if (line == null) {
      return;
    }

    if (expectRevisionLine) {
      // this is the revision line
      consumeRevisionLine(line);
    } else {

      if (extractCommitInfoFromLine(line)) {
        return;
      }

      if (line.startsWith("\t")) {
        // this is the content line.
        // we actually don't need the content, but this is the right time to add the blame line
        consumeContentLine();
      }
    }
  }

  @VisibleForTesting
  protected boolean extractCommitInfoFromLine(String line) {
    if (line.startsWith(GIT_AUTHOR_EMAIL)) {
      author = extractEmail(line);
      return true;
    }

    if (line.startsWith(GIT_COMMITTER_EMAIL)) {
      committer = extractEmail(line);
      return true;
    }

    if (line.startsWith(GIT_COMMITTER_TIME)) {
      String timeStr = line.substring(GIT_COMMITTER_TIME.length());
      time = new Date(Long.parseLong(timeStr) * 1000L);
      return true;
    }
    return false;
  }

  private String extractEmail(String line) {

    int emailStartIndex = line.indexOf(OPENING_EMAIL_FIELD);
    int emailEndIndex = line.indexOf(CLOSING_EMAIL_FIELD);

    if (emailStartIndex == -1 || emailEndIndex == -1 || emailEndIndex <= emailStartIndex) {
      return null;
    }
    return line.substring(emailStartIndex + 1, emailEndIndex);
  }

  private void consumeContentLine() {
    BlameLine blameLine = new BlameLine(time, revision, author, committer);
    getLines().add(blameLine);

    // keep commitinfo for this sha-1
    commitInfo.put(revision, blameLine);

    expectRevisionLine = true;
  }

  private void consumeRevisionLine(String line) {
    String[] parts = line.split("\\s", 4);

    if (parts.length >= 1) {
      revision = parts[0];

      if (StringUtils.containsOnly(revision, "0")) {
        throw new IllegalStateException("Unable to blame file " + filename + ". No blame info at line " + (getLines().size() + 1) + ". Is file commited?");
      }

      BlameLine oldLine = commitInfo.get(revision);

      if (oldLine != null) {
        // restore the commit info
        author = oldLine.getAuthor();
        committer = oldLine.getCommitter();
        time = oldLine.getDate();
      }

      expectRevisionLine = false;
    }
  }

  public List<BlameLine> getLines() {
    return lines;
  }
}
