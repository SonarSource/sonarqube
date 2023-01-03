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
package org.sonar.scm.svn;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.sonar.api.batch.scm.BlameLine;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.ISVNAnnotateHandler;

public class AnnotationHandler implements ISVNAnnotateHandler {

  private List<BlameLine> lines = new ArrayList<>();

  @Override
  public void handleEOF() {
    // Not used
  }

  @Override
  public void handleLine(Date date, long revision, String author, String line) throws SVNException {
    // deprecated
  }

  @Override
  public void handleLine(Date date, long revision, String author, String line, Date mergedDate,
    long mergedRevision, String mergedAuthor, String mergedPath, int lineNumber) throws SVNException {
    lines.add(new BlameLine().date(mergedDate).revision(Long.toString(mergedRevision)).author(mergedAuthor));
  }

  @Override
  public boolean handleRevision(Date date, long revision, String author, File contents) throws SVNException {
    /*
     * We do not want our file to be annotated for each revision of the range, but only for the last
     * revision of it, so we return false
     */
    return false;
  }

  public List<BlameLine> getLines() {
    return lines;
  }

}
