/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.cpd.deprecated;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.CpdMapping;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.internal.pmd.TokenizerBridge;
import org.sonar.scanner.cpd.index.SonarCpdBlockIndex;

public class DefaultCpdBlockIndexer extends CpdBlockIndexer {

  private static final Logger LOG = Loggers.get(DefaultCpdBlockIndexer.class);

  private final CpdMappings mappings;
  private final FileSystem fs;
  private final Configuration settings;
  private final SonarCpdBlockIndex index;

  public DefaultCpdBlockIndexer(CpdMappings mappings, FileSystem fs, Configuration settings, SonarCpdBlockIndex index) {
    this.mappings = mappings;
    this.fs = fs;
    this.settings = settings;
    this.index = index;
  }

  @Override
  public boolean isLanguageSupported(String language) {
    return true;
  }

  @Override
  public void index(String languageKey) {
    CpdMapping mapping = mappings.getMapping(languageKey);
    if (mapping == null) {
      LOG.debug("No CpdMapping for language {}", languageKey);
      return;
    }

    String[] cpdExclusions = settings.getStringArray(CoreProperties.CPD_EXCLUSIONS);
    logExclusions(cpdExclusions);
    FilePredicates p = fs.predicates();
    List<InputFile> sourceFiles = Lists.newArrayList(fs.inputFiles(p.and(
      p.hasType(InputFile.Type.MAIN),
      p.hasLanguage(languageKey),
      p.doesNotMatchPathPatterns(cpdExclusions))));
    if (sourceFiles.isEmpty()) {
      return;
    }

    // Create index
    populateIndex(languageKey, sourceFiles, mapping);
  }

  private void populateIndex(String languageKey, List<InputFile> sourceFiles, CpdMapping mapping) {
    TokenizerBridge bridge = new TokenizerBridge(mapping.getTokenizer(), getBlockSize(languageKey));
    for (InputFile inputFile : sourceFiles) {
      if (!index.isIndexed(inputFile)) {
        LOG.debug("Populating index from {}", inputFile.absolutePath());
        String resourceEffectiveKey = ((DefaultInputFile) inputFile).key();
        List<Block> blocks;
        try (InputStreamReader isr = new InputStreamReader(inputFile.inputStream(), inputFile.charset())) {
          blocks = bridge.chunk(resourceEffectiveKey, inputFile.absolutePath(), isr);
        } catch (IOException e) {
          throw new IllegalStateException("Unable to read content of file " + inputFile.absolutePath(), e);
        }
        index.insert(inputFile, blocks);
      }
    }
  }

  @VisibleForTesting
  int getBlockSize(String languageKey) {
    return settings.getInt("sonar.cpd." + languageKey + ".minimumLines").orElse(getDefaultBlockSize(languageKey));
  }

  @VisibleForTesting
  public static int getDefaultBlockSize(String languageKey) {
    if ("cobol".equals(languageKey)) {
      return 30;
    } else if ("abap".equals(languageKey)) {
      return 20;
    } else {
      return 10;
    }
  }

}
