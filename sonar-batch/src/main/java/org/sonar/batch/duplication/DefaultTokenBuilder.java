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
package org.sonar.batch.duplication;

import com.google.common.base.Preconditions;
import net.sourceforge.pmd.cpd.TokenEntry;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.duplication.DuplicationTokenBuilder;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.FileBlocks;
import org.sonar.duplications.internal.pmd.PmdBlockChunker;
import org.sonar.duplications.internal.pmd.TokenizerBridge;
import org.sonar.duplications.internal.pmd.TokensLine;

import java.util.ArrayList;
import java.util.List;

public class DefaultTokenBuilder implements DuplicationTokenBuilder {

  private final BlockCache cache;
  private final InputFile inputFile;
  private final List<TokenEntry> tokens = new ArrayList<TokenEntry>();
  private final PmdBlockChunker blockChunker;
  private boolean done = false;
  private int previousLine = 0;

  public DefaultTokenBuilder(InputFile inputFile, BlockCache cache, PmdBlockChunker blockChunker) {
    this.inputFile = inputFile;
    this.cache = cache;
    this.blockChunker = blockChunker;
    TokenEntry.clearImages();
  }

  @Override
  public DefaultTokenBuilder addToken(int line, String image) {
    Preconditions.checkState(!done, "done() already called");
    Preconditions.checkState(line >= previousLine, "Token should be created in order. Previous line was " + previousLine + " and you tried to create a token at line " + line);
    TokenEntry cpdToken = new TokenEntry(image, inputFile.absolutePath(), line);
    tokens.add(cpdToken);
    previousLine = line;
    return this;
  }

  @Override
  public void done() {
    Preconditions.checkState(!done, "done() already called");
    tokens.add(TokenEntry.getEOF());
    TokenEntry.clearImages();
    List<TokensLine> tokensLines = TokenizerBridge.convert(tokens);
    List<Block> blocks = blockChunker.chunk(((DefaultInputFile) inputFile).key(), tokensLines);

    cache.put(((DefaultInputFile) inputFile).key(), new FileBlocks(((DefaultInputFile) inputFile).key(), blocks));
    tokens.clear();
  }
}
