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
package org.sonar.ce.task.projectanalysis.duplication;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.detector.suffixtree.SuffixTreeCloneDetectionAlgorithm;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.CloneIndex;
import org.sonar.duplications.index.ClonePart;
import org.sonar.duplications.index.PackedMemoryCloneIndex;

import static com.google.common.collect.FluentIterable.from;

/**
 * Transform a list of duplication blocks into clone groups, then add these clone groups into the duplication repository.
 */
public class IntegrateCrossProjectDuplications {

  private static final Logger LOGGER = Loggers.get(IntegrateCrossProjectDuplications.class);

  private static final String JAVA_KEY = "java";

  private static final int MAX_CLONE_GROUP_PER_FILE = 100;
  private static final int MAX_CLONE_PART_PER_GROUP = 100;

  private final Configuration config;
  private final DuplicationRepository duplicationRepository;

  private Map<String, NumberOfUnitsNotLessThan> numberOfUnitsByLanguage = new HashMap<>();

  public IntegrateCrossProjectDuplications(Configuration config, DuplicationRepository duplicationRepository) {
    this.config = config;
    this.duplicationRepository = duplicationRepository;
    if (config.getBoolean(CoreProperties.CPD_CROSS_PROJECT).orElse(false)) {
      LOGGER.warn("This analysis uses the deprecated cross-project duplication feature.");
    }
  }

  public void computeCpd(Component component, Collection<Block> originBlocks, Collection<Block> duplicationBlocks) {
    CloneIndex duplicationIndex = new PackedMemoryCloneIndex();
    populateIndex(duplicationIndex, originBlocks);
    populateIndex(duplicationIndex, duplicationBlocks);

    List<CloneGroup> duplications = SuffixTreeCloneDetectionAlgorithm.detect(duplicationIndex, originBlocks);
    Iterable<CloneGroup> filtered = from(duplications).filter(getNumberOfUnitsNotLessThan(component.getFileAttributes().getLanguageKey()));
    addDuplications(component, filtered);
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
        LOGGER.warn("Too many duplication groups on file {}. Keeping only the first {} groups.", file.getDbKey(), MAX_CLONE_GROUP_PER_FILE);
        break;
      }
      addDuplication(file, duplication);
    }
  }

  private void addDuplication(Component file, CloneGroup duplication) {
    ClonePart originPart = duplication.getOriginPart();
    Iterable<Duplicate> duplicates = convertClonePartsToDuplicates(file, duplication);
    if (!Iterables.isEmpty(duplicates)) {
      duplicationRepository.add(
        file,
        new Duplication(new TextBlock(originPart.getStartLine(), originPart.getEndLine()), duplicates));
    }
  }

  private static Iterable<Duplicate> convertClonePartsToDuplicates(final Component file, CloneGroup duplication) {
    final ClonePart originPart = duplication.getOriginPart();
    return from(duplication.getCloneParts())
      .filter(new DoesNotMatchSameComponentKey(originPart.getResourceId()))
      .filter(new DuplicateLimiter(file, originPart))
      .transform(ClonePartToCrossProjectDuplicate.INSTANCE);
  }

  private NumberOfUnitsNotLessThan getNumberOfUnitsNotLessThan(String language) {
    NumberOfUnitsNotLessThan numberOfUnitsNotLessThan = numberOfUnitsByLanguage.get(language);
    if (numberOfUnitsNotLessThan == null) {
      numberOfUnitsNotLessThan = new NumberOfUnitsNotLessThan(getMinimumTokens(language));
      numberOfUnitsByLanguage.put(language, numberOfUnitsNotLessThan);
    }
    return numberOfUnitsNotLessThan;
  }

  private int getMinimumTokens(String languageKey) {
    // The java language is an exception : it doesn't compute tokens but statement, so the settings could not be used.
    if (languageKey.equalsIgnoreCase(JAVA_KEY)) {
      return 0;
    }
    return config.getInt("sonar.cpd." + languageKey + ".minimumTokens").orElse(100);
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

  private static class DoesNotMatchSameComponentKey implements Predicate<ClonePart> {
    private final String componentKey;

    private DoesNotMatchSameComponentKey(String componentKey) {
      this.componentKey = componentKey;
    }

    @Override
    public boolean apply(@Nonnull ClonePart part) {
      return !part.getResourceId().equals(componentKey);
    }
  }

  private static class DuplicateLimiter implements Predicate<ClonePart> {
    private final Component file;
    private final ClonePart originPart;
    private int counter = 0;

    public DuplicateLimiter(Component file, ClonePart originPart) {
      this.file = file;
      this.originPart = originPart;
    }

    @Override
    public boolean apply(@Nonnull ClonePart input) {
      if (counter == MAX_CLONE_PART_PER_GROUP) {
        LOGGER.warn("Too many duplication references on file {} for block at line {}. Keeping only the first {} references.",
          file.getDbKey(), originPart.getStartLine(), MAX_CLONE_PART_PER_GROUP);
      }
      boolean res = counter <= MAX_CLONE_GROUP_PER_FILE;
      counter++;
      return res;
    }
  }

  private enum ClonePartToCrossProjectDuplicate implements Function<ClonePart, Duplicate> {
    INSTANCE;

    @Override
    @Nonnull
    public Duplicate apply(@Nonnull ClonePart input) {
      return new CrossProjectDuplicate(
        input.getResourceId(),
        new TextBlock(input.getStartLine(), input.getEndLine()));
    }
  }
}
