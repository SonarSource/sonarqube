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
package org.sonar.xoo.lang;

import com.google.common.base.Splitter;
import net.sourceforge.pmd.cpd.SourceCode;
import net.sourceforge.pmd.cpd.TokenEntry;
import net.sourceforge.pmd.cpd.Tokenizer;
import net.sourceforge.pmd.cpd.Tokens;
import org.apache.commons.io.FileUtils;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.fs.FileSystem;

import java.io.File;
import java.io.IOException;

@BatchSide
public class XooTokenizer implements Tokenizer {

  private FileSystem fs;

  public XooTokenizer(FileSystem fs) {
    this.fs = fs;
  }

  @Override
  public final void tokenize(SourceCode source, Tokens cpdTokens) {
    String fileName = source.getFileName();
    int lineIdx = 1;
    try {
      for (String line : FileUtils.readLines(new File(fileName), fs.encoding())) {
        for (String token : Splitter.on(" ").split(line)) {
          TokenEntry cpdToken = new TokenEntry(token, fileName, lineIdx);
          cpdTokens.add(cpdToken);
        }
        lineIdx++;
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to tokenize", e);
    }
    cpdTokens.add(TokenEntry.getEOF());
  }
}
