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

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.scm.BlameCommand;
import org.sonar.api.batch.scm.BlameLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNDiffOptions;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;

import static org.sonar.scm.svn.SvnScmSupport.newSvnClientManager;

public class SvnBlameCommand extends BlameCommand {

  private static final Logger LOG = LoggerFactory.getLogger(SvnBlameCommand.class);
  private final SvnConfiguration configuration;

  public SvnBlameCommand(SvnConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void blame(final BlameInput input, final BlameOutput output) {
    FileSystem fs = input.fileSystem();
    LOG.debug("Working directory: " + fs.baseDir().getAbsolutePath());
    SVNClientManager clientManager = null;
    try {
      clientManager = newSvnClientManager(configuration);
      for (InputFile inputFile : input.filesToBlame()) {
        blame(clientManager, inputFile, output);
      }
    } finally {
      if (clientManager != null) {
        try {
          clientManager.dispose();
        } catch (Exception e) {
          LOG.warn("Unable to dispose SVN ClientManager", e);
        }
      }
    }
  }

  @VisibleForTesting
  void blame(SVNClientManager clientManager, InputFile inputFile, BlameOutput output) {
    String filename = inputFile.relativePath();

    LOG.debug("Process file {}", filename);

    AnnotationHandler handler = new AnnotationHandler();
    try {
      if (!checkStatus(clientManager, inputFile)) {
        return;
      }
      SVNLogClient logClient = clientManager.getLogClient();
      logClient.setDiffOptions(new SVNDiffOptions(true, true, true));
      logClient.doAnnotate(inputFile.file(), SVNRevision.UNDEFINED, SVNRevision.create(1), SVNRevision.BASE, true, true, handler, null);
    } catch (SVNAuthenticationException e) {
      if(configuration.isEmpty()) {
        LOG.warn("Authentication to SVN server is required but no authentication data was passed to the scanner");
      }
      throw new IllegalStateException("Authentication error when executing blame for file " + filename, e);
    } catch (SVNException e) {
      throw new IllegalStateException("Error when executing blame for file " + filename, e);
    }

    List<BlameLine> lines = handler.getLines();
    if (lines.size() == inputFile.lines() - 1) {
      // SONARPLUGINS-3097 SVN do not report blame on last empty line
      lines.add(lines.get(lines.size() - 1));
    }
    output.blameResult(inputFile, lines);
  }

  private static boolean checkStatus(SVNClientManager clientManager, InputFile inputFile) throws SVNException {
    SVNStatusClient statusClient = clientManager.getStatusClient();
    try {
      SVNStatus status = statusClient.doStatus(inputFile.file(), false);
      if (status == null) {
        LOG.debug("File {} returns no svn state. Skipping it.", inputFile);
        return false;
      }
      if (status.getContentsStatus() != SVNStatusType.STATUS_NORMAL) {
        LOG.debug("File {} is not versionned or contains local modifications. Skipping it.", inputFile);
        return false;
      }
    } catch (SVNException e) {
      if (SVNErrorCode.WC_PATH_NOT_FOUND.equals(e.getErrorMessage().getErrorCode())
        || SVNErrorCode.WC_NOT_WORKING_COPY.equals(e.getErrorMessage().getErrorCode())) {
        LOG.debug("File {} is not versionned. Skipping it.", inputFile);
        return false;
      }
      throw e;
    }
    return true;
  }
}
