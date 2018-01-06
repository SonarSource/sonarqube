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

import com.google.common.collect.Lists;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.BlockChunker;
import org.sonar.duplications.java.JavaStatementBuilder;
import org.sonar.duplications.java.JavaTokenProducer;
import org.sonar.duplications.statement.Statement;
import org.sonar.duplications.statement.StatementChunker;
import org.sonar.duplications.token.TokenChunker;
import org.sonar.scanner.cpd.index.SonarCpdBlockIndex;

public class JavaCpdBlockIndexer extends CpdBlockIndexer {

  private static final Logger LOG = Loggers.get(JavaCpdBlockIndexer.class);

  private static final int BLOCK_SIZE = 10;

  private final FileSystem fs;
  private final Configuration settings;
  private final SonarCpdBlockIndex index;

  public JavaCpdBlockIndexer(FileSystem fs, Configuration settings, SonarCpdBlockIndex index) {
    this.fs = fs;
    this.settings = settings;
    this.index = index;
  }

  @Override
  public boolean isLanguageSupported(String language) {
    return "java".equals(language);
  }

  @Override
  public void index(String languageKey) {
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
    createIndex(sourceFiles);
  }

  private void createIndex(Iterable<InputFile> sourceFiles) {
    TokenChunker tokenChunker = JavaTokenProducer.build();
    StatementChunker statementChunker = JavaStatementBuilder.build();
    BlockChunker blockChunker = new BlockChunker(BLOCK_SIZE);

    for (InputFile inputFile : sourceFiles) {
      LOG.debug("Populating index from {}", inputFile);
      String resourceEffectiveKey = ((DefaultInputFile) inputFile).key();

      List<Statement> statements;

      try (InputStream is = inputFile.inputStream();
        Reader reader = new InputStreamReader(is, inputFile.charset())) {
        statements = statementChunker.chunk(tokenChunker.chunk(reader));
      } catch (FileNotFoundException e) {
        throw new IllegalStateException("Cannot find file " + inputFile.file(), e);
      } catch (IOException e) {
        throw new IllegalStateException("Exception handling file: " + inputFile.file(), e);
      }

      List<Block> blocks;
      try {
        blocks = blockChunker.chunk(resourceEffectiveKey, statements);
      } catch (Exception e) {
        throw new IllegalStateException("Cannot process file " + inputFile.file(), e);
      }
      index.insert(inputFile, blocks);
    }
  }
}
