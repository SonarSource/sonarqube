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

package org.sonar.server.computation.duplication;

import com.google.common.base.Predicate;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.detector.suffixtree.SuffixTreeCloneDetectionAlgorithm;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.CloneIndex;
import org.sonar.duplications.index.ClonePart;
import org.sonar.duplications.index.PackedMemoryCloneIndex;
import org.sonar.server.computation.component.Component;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.isEmpty;

/**
 * Transform a list of duplication blocks into clone groups, then add these clone groups into the duplication repository.
 */
public class IntegrateCrossProjectDuplications {

  private static final Logger LOGGER = Loggers.get(IntegrateCrossProjectDuplications.class);

  private static final String JAVA_KEY = "java";

  private static final int MAX_CLONE_GROUP_PER_FILE = 100;
  private static final int MAX_CLONE_PART_PER_GROUP = 100;

  private final Settings settings;
  private final DuplicationRepository duplicationRepository;

  public IntegrateCrossProjectDuplications(Settings settings, DuplicationRepository duplicationRepository) {
    this.settings = settings;
    this.duplicationRepository = duplicationRepository;
  }

  public void computeCpd(Component component, Collection<Block> originBlocks, Collection<Block> duplicationBlocks) {
    CloneIndex duplicationIndex = new PackedMemoryCloneIndex();
    populateIndex(duplicationIndex, originBlocks);
    populateIndex(duplicationIndex, duplicationBlocks);

    List<CloneGroup> duplications = SuffixTreeCloneDetectionAlgorithm.detect(duplicationIndex, originBlocks);
    Iterable<CloneGroup> filtered = from(duplications).filter(new NumberOfUnitsNotLessThan(getMinimumTokens(component.getFileAttributes().getLanguageKey())));
    if (isEmpty(filtered)) {
      return;
    }
    addDuplications(component, duplications);
  }

  private static void populateIndex(CloneIndex duplicationIndex, Collection<Block> duplicationBlocks) {
    for (Block block : duplicationBlocks) {
      duplicationIndex.insert(block);
    }
  }

  private void addDuplications(Component file, Iterable<CloneGroup> duplications) {
    int cloneGroupCount = 0;
    for (CloneGroup duplication : duplications) {
      cloneGroupCount++;
      if (cloneGroupCount > MAX_CLONE_GROUP_PER_FILE) {
        LOGGER.warn("Too many duplication groups on file {}. Keep only the first {} groups.", file.getKey(), MAX_CLONE_GROUP_PER_FILE);
        break;
      }
      addDuplication(file, duplication);
    }
  }

  private void addDuplication(Component file, CloneGroup duplication) {
    ClonePart originPart = duplication.getOriginPart();
    TextBlock originTextBlock = new TextBlock(originPart.getStartLine(), originPart.getEndLine());
    int clonePartCount = 0;
    for (ClonePart part : duplication.getCloneParts()) {
      if (!part.equals(originPart)) {
        clonePartCount++;
        if (clonePartCount > MAX_CLONE_PART_PER_GROUP) {
          LOGGER.warn("Too many duplication references on file {} for block at line {}. Keep only the first {} references.",
            file.getKey(), originPart.getStartLine(), MAX_CLONE_PART_PER_GROUP);
          break;
        }
        duplicationRepository.addDuplication(file, originTextBlock, part.getResourceId(),
          new TextBlock(part.getStartLine(), part.getEndLine()));
      }
    }
  }

  private int getMinimumTokens(String languageKey) {
    if (languageKey.equalsIgnoreCase(JAVA_KEY)) {
      return 0;
    }
    int minimumTokens = settings.getInt("sonar.cpd." + languageKey + ".minimumTokens");
    if (minimumTokens == 0) {
      return 100;
    }
    return minimumTokens;
  }

  private static class NumberOfUnitsNotLessThan implements Predicate<CloneGroup> {
    private final int min;

    public NumberOfUnitsNotLessThan(int min) {
      this.min = min;
    }

    @Override
    public boolean apply(@Nonnull CloneGroup input) {
      return input.getLengthInUnits() >= min;
    }
  }
}
