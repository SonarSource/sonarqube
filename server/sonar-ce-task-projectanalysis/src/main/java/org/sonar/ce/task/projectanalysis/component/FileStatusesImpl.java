/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.source.SourceHashRepository;
import org.sonar.db.source.FileHashesDto;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.unmodifiableSet;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

public class FileStatusesImpl implements FileStatuses {
  private final PreviousSourceHashRepository previousSourceHashRepository;
  private final SourceHashRepository sourceHashRepository;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final TreeRootHolder treeRootHolder;
  private Set<String> fileUuidsMarkedAsUnchanged;

  public FileStatusesImpl(AnalysisMetadataHolder analysisMetadataHolder, TreeRootHolder treeRootHolder, PreviousSourceHashRepository previousSourceHashRepository,
    SourceHashRepository sourceHashRepository) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.treeRootHolder = treeRootHolder;
    this.previousSourceHashRepository = previousSourceHashRepository;
    this.sourceHashRepository = sourceHashRepository;
  }

  public void initialize() {
    fileUuidsMarkedAsUnchanged = new HashSet<>();
    if (!analysisMetadataHolder.isPullRequest() && !analysisMetadataHolder.isFirstAnalysis()) {
      new DepthTraversalTypeAwareCrawler(new Visitor()).visit(treeRootHolder.getRoot());
    }
  }

  private class Visitor extends TypeAwareVisitorAdapter {
    private boolean canTrustUnchangedFlags = true;

    private Visitor() {
      super(CrawlerDepthLimit.FILE, PRE_ORDER);
    }

    @Override
    public void visitFile(Component file) {
      if (file.getStatus() != Component.Status.SAME || !canTrustUnchangedFlags) {
        return;
      }

      canTrustUnchangedFlags = hashEquals(file);
      if (canTrustUnchangedFlags) {
        if (file.getFileAttributes().isMarkedAsUnchanged()) {
          fileUuidsMarkedAsUnchanged.add(file.getUuid());
        }
      } else {
        fileUuidsMarkedAsUnchanged.clear();
      }
    }
  }

  @Override
  public boolean isUnchanged(Component component) {
    failIfNotInitialized();
    return component.getStatus() == Component.Status.SAME && hashEquals(component);
  }

  @Override
  public boolean isDataUnchanged(Component component) {
    failIfNotInitialized();
    return fileUuidsMarkedAsUnchanged.contains(component.getUuid());
  }

  @Override
  public Set<String> getFileUuidsMarkedAsUnchanged() {
    failIfNotInitialized();
    return unmodifiableSet(fileUuidsMarkedAsUnchanged);
  }

  private boolean hashEquals(Component component) {
    Optional<String> dbHash = previousSourceHashRepository.getDbFile(component).map(FileHashesDto::getSrcHash);
    return dbHash.map(hash -> hash.equals(sourceHashRepository.getRawSourceHash(component))).orElse(false);
  }

  private void failIfNotInitialized() {
    checkState(fileUuidsMarkedAsUnchanged != null, "Not initialized");
  }
}
