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
package org.sonar.scanner.cpd;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.scanner.sensor.ProjectSensor;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.BlockChunker;
import org.sonar.duplications.java.JavaStatementBuilder;
import org.sonar.duplications.java.JavaTokenProducer;
import org.sonar.duplications.statement.Statement;
import org.sonar.duplications.statement.StatementChunker;
import org.sonar.duplications.token.TokenChunker;
import org.sonar.scanner.cpd.index.SonarCpdBlockIndex;

/**
 * Special case for Java that use a dedicated block indexer.
 */
@Phase(name = Phase.Name.POST)
public class JavaCpdBlockIndexerSensor implements ProjectSensor {

  private static final int BLOCK_SIZE = 10;
  private static final Logger LOG = LoggerFactory.getLogger(JavaCpdBlockIndexerSensor.class);
  private final SonarCpdBlockIndex index;

  public JavaCpdBlockIndexerSensor(SonarCpdBlockIndex index) {
    this.index = index;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Java CPD Block Indexer")
      .onlyOnLanguage("java");
  }

  @Override
  public void execute(SensorContext context) {
    FilePredicates p = context.fileSystem().predicates();
    List<InputFile> sourceFiles = StreamSupport.stream(
      context.fileSystem().inputFiles(
        p.and(
          p.hasType(InputFile.Type.MAIN),
          p.hasLanguage("java")
        )
      ).spliterator(), false)
      .filter(f -> !((DefaultInputFile) f).isExcludedForDuplication())
      .collect(Collectors.toList());
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
      String resourceEffectiveKey = inputFile.key();

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
