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

import org.apache.commons.configuration.Configuration;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.CpdMapping;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.resources.*;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.BlockChunker;
import org.sonar.duplications.detector.suffixtree.SuffixTreeCloneDetectionAlgorithm;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.CloneIndex;
import org.sonar.duplications.index.PackedMemoryCloneIndex;

import java.util.Collection;
import java.util.List;

public class SonarBridgeEngine extends CpdEngine {

  private final CpdMapping[] mappings;

  public SonarBridgeEngine() {
    this.mappings = null;
  }

  public SonarBridgeEngine(CpdMapping[] mappings) {
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
    BlockChunker blockChunker = new BlockChunker(getMinimumTokens(project));
    CloneIndex index = new PackedMemoryCloneIndex();
    TokenizerBridge bridge = new TokenizerBridge(mapping.getTokenizer(), fileSystem.getSourceCharset().name());
    for (InputFile inputFile : inputFiles) {
      Resource resource = mapping.createResource(inputFile.getFile(), fileSystem.getSourceDirs());
      String resourceId = getFullKey(project, resource);
      List<Block> blocks = blockChunker.chunk(resourceId, bridge.tokenize(inputFile.getFile()));
      for (Block block : blocks) {
        index.insert(block);
      }
    }
    bridge.clearCache();

    // Detect
    for (InputFile inputFile : inputFiles) {
      Resource resource = mapping.createResource(inputFile.getFile(), fileSystem.getSourceDirs());
      String resourceId = getFullKey(project, resource);
      Collection<Block> fileBlocks = index.getByResourceId(resourceId);
      List<CloneGroup> duplications = SuffixTreeCloneDetectionAlgorithm.detect(index, fileBlocks);
      SonarEngine.save(context, resource, duplications);
    }
  }

  private static String getFullKey(Project project, Resource resource) {
    return new StringBuilder(ResourceModel.KEY_SIZE)
        .append(project.getKey())
        .append(':')
        .append(resource.getKey())
        .toString();
  }

  private int getMinimumTokens(Project project) {
    Configuration conf = project.getConfiguration();
    return conf.getInt("sonar.cpd." + project.getLanguageKey() + ".minimumTokens",
        conf.getInt("sonar.cpd.minimumTokens", CoreProperties.CPD_MINIMUM_TOKENS_DEFAULT_VALUE));
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
