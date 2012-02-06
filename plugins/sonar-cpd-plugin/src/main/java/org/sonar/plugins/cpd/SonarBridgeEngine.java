/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.cpd;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.sonar.api.batch.CpdMapping;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.*;
import org.sonar.duplications.DuplicationPredicates;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.detector.suffixtree.SuffixTreeCloneDetectionAlgorithm;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.internal.pmd.TokenizerBridge;
import org.sonar.plugins.cpd.index.IndexFactory;
import org.sonar.plugins.cpd.index.SonarDuplicationsIndex;

import java.util.Collection;
import java.util.List;

public class SonarBridgeEngine extends CpdEngine {

  private final IndexFactory indexFactory;
  private final CpdMapping[] mappings;

  public SonarBridgeEngine(IndexFactory indexFactory) {
    this(indexFactory, null);
  }

  public SonarBridgeEngine(IndexFactory indexFactory, CpdMapping[] mappings) {
    this.indexFactory = indexFactory;
    this.mappings = mappings;
  }

  @Override
  public boolean isLanguageSupported(Language language) {
    return getMapping(language) != null;
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    ProjectFileSystem fileSystem = project.getFileSystem();
    List<InputFile> inputFiles = fileSystem.mainFiles(project.getLanguageKey());
    if (inputFiles.isEmpty()) {
      return;
    }

    CpdMapping mapping = getMapping(project.getLanguage());

    // Create index
    SonarDuplicationsIndex index = indexFactory.create(project);

    TokenizerBridge bridge = new TokenizerBridge(mapping.getTokenizer(), fileSystem.getSourceCharset().name());
    for (InputFile inputFile : inputFiles) {
      Resource resource = mapping.createResource(inputFile.getFile(), fileSystem.getSourceDirs());
      String resourceId = SonarEngine.getFullKey(project, resource);
      List<Block> blocks = bridge.chunk(resourceId, inputFile.getFile());
      index.insert(resource, blocks);
    }

    // Detect
    Predicate<CloneGroup> minimumTokensPredicate = DuplicationPredicates.numberOfUnitsNotLessThan(PmdEngine.getMinimumTokens(project));

    for (InputFile inputFile : inputFiles) {
      Resource resource = mapping.createResource(inputFile.getFile(), fileSystem.getSourceDirs());
      String resourceKey = SonarEngine.getFullKey(project, resource);

      Collection<Block> fileBlocks = index.getByResource(resource, resourceKey);
      List<CloneGroup> duplications = SuffixTreeCloneDetectionAlgorithm.detect(index, fileBlocks);

      Iterable<CloneGroup> filtered = Iterables.filter(duplications, minimumTokensPredicate);

      SonarEngine.save(context, resource, filtered);
    }
  }

  private CpdMapping getMapping(Language language) {
    if (mappings != null) {
      for (CpdMapping cpdMapping : mappings) {
        if (cpdMapping.getLanguage().equals(language)) {
          return cpdMapping;
        }
      }
    }
    return null;
  }

}
