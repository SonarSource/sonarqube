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
package org.sonar.ce.task.projectanalysis.component;

import javax.annotation.Nullable;
import org.sonar.api.utils.MessageException;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolder;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.sonar.scanner.protocol.output.ScannerReport.Metadata.BranchType.UNSET;

public class BranchLoader {
  private final MutableAnalysisMetadataHolder metadataHolder;
  private final BranchLoaderDelegate delegate;

  public BranchLoader(MutableAnalysisMetadataHolder metadataHolder) {
    this(metadataHolder, null);
  }

  public BranchLoader(MutableAnalysisMetadataHolder metadataHolder, @Nullable BranchLoaderDelegate delegate) {
    this.metadataHolder = metadataHolder;
    this.delegate = delegate;
  }

  public void load(ScannerReport.Metadata metadata) {
    if (delegate != null) {
      delegate.load(metadata);
    } else if (hasBranchProperties(metadata)) {
      throw MessageException.of("Current edition does not support branch feature");
    } else {
      metadataHolder.setBranch(new DefaultBranchImpl());
    }
  }

  private static boolean hasBranchProperties(ScannerReport.Metadata metadata) {
    return !metadata.getBranchName().isEmpty()
      || !metadata.getPullRequestKey().isEmpty()
      || !metadata.getMergeBranchName().isEmpty()
      || metadata.getBranchType() != UNSET;
  }

}
