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
package org.sonar.api.batch.sensor.symbol.internal;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.DefaultStorable;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.symbol.NewSymbol;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;

import static java.util.Objects.requireNonNull;
import static org.sonar.api.utils.Preconditions.checkArgument;
import static org.sonar.api.utils.Preconditions.checkState;

public class DefaultSymbolTable extends DefaultStorable implements NewSymbolTable {

  private final Map<TextRange, Set<TextRange>> referencesBySymbol;
  private DefaultInputFile inputFile;

  public DefaultSymbolTable(SensorStorage storage) {
    super(storage);
    referencesBySymbol = new LinkedHashMap<>();
  }

  public Map<TextRange, Set<TextRange>> getReferencesBySymbol() {
    return referencesBySymbol;
  }

  @Override
  public DefaultSymbolTable onFile(InputFile inputFile) {
    requireNonNull(inputFile, "file can't be null");
    this.inputFile = (DefaultInputFile) inputFile;
    return this;
  }

  public InputFile inputFile() {
    return inputFile;
  }

  @Override
  public NewSymbol newSymbol(int startLine, int startLineOffset, int endLine, int endLineOffset) {
    checkInputFileNotNull();
    TextRange declarationRange;
    try {
      declarationRange = inputFile.newRange(startLine, startLineOffset, endLine, endLineOffset);
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to create symbol on file " + inputFile, e);
    }
    return newSymbol(declarationRange);
  }

  @Override
  public NewSymbol newSymbol(int startOffset, int endOffset) {
    checkInputFileNotNull();
    TextRange declarationRange;
    try {
      declarationRange = inputFile.newRange(startOffset, endOffset);
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to create symbol on file " + inputFile, e);
    }
    return newSymbol(declarationRange);
  }

  @Override
  public NewSymbol newSymbol(TextRange range) {
    checkInputFileNotNull();
    TreeSet<TextRange> references = new TreeSet<>((o1, o2) -> o1.start().compareTo(o2.start()));
    referencesBySymbol.put(range, references);
    return new DefaultSymbol(inputFile, range, references);
  }

  private static class DefaultSymbol implements NewSymbol {

    private final Collection<TextRange> references;
    private final DefaultInputFile inputFile;
    private final TextRange declaration;

    public DefaultSymbol(DefaultInputFile inputFile, TextRange declaration, Collection<TextRange> references) {
      this.inputFile = inputFile;
      this.declaration = declaration;
      this.references = references;
    }

    @Override
    public NewSymbol newReference(int startOffset, int endOffset) {
      TextRange referenceRange;
      try {
        referenceRange = inputFile.newRange(startOffset, endOffset);
      } catch (Exception e) {
        throw new IllegalArgumentException("Unable to create symbol reference on file " + inputFile, e);
      }
      return newReference(referenceRange);
    }

    @Override
    public NewSymbol newReference(int startLine, int startLineOffset, int endLine, int endLineOffset) {
      TextRange referenceRange;
      try {
        referenceRange = inputFile.newRange(startLine, startLineOffset, endLine, endLineOffset);
      } catch (Exception e) {
        throw new IllegalArgumentException("Unable to create symbol reference on file " + inputFile, e);
      }
      return newReference(referenceRange);
    }

    @Override
    public NewSymbol newReference(TextRange range) {
      requireNonNull(range, "Provided range is null");
      checkArgument(!declaration.overlap(range), "Overlapping symbol declaration and reference for symbol at %s", declaration);
      references.add(range);
      return this;
    }

  }

  @Override
  protected void doSave() {
    checkInputFileNotNull();
    storage.store(this);
  }

  private void checkInputFileNotNull() {
    checkState(inputFile != null, "Call onFile() first");
  }
}
