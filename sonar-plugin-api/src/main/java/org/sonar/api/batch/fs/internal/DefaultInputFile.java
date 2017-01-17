/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.api.batch.fs.internal;

import com.google.common.base.Preconditions;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Function;

import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;

/**
 * @since 4.2
 */
public class DefaultInputFile extends DefaultInputComponent implements InputFile {
  private final DefaultIndexedFile indexedFile;
  private final Function<DefaultInputFile, Metadata> metadataGenerator;
  private Status status;
  private Charset charset;
  private Metadata metadata;

  public DefaultInputFile(DefaultIndexedFile indexedFile, Function<DefaultInputFile, Metadata> metadataGenerator) {
    super(indexedFile.batchId());
    this.indexedFile = indexedFile;
    this.metadataGenerator = metadataGenerator;
    this.metadata = null;
  }

  private void checkMetadata() {
    if (metadata == null) {
      setMetadata(metadataGenerator.apply(this));
    }
  }

  @Override
  public String relativePath() {
    return indexedFile.relativePath();
  }

  @Override
  public String absolutePath() {
    return indexedFile.absolutePath();
  }

  @Override
  public File file() {
    return indexedFile.file();
  }

  @Override
  public Path path() {
    return indexedFile.path();
  }

  @CheckForNull
  @Override
  public String language() {
    return indexedFile.language();
  }

  @Override
  public Type type() {
    return indexedFile.type();
  }

  /**
   * Component key.
   */
  @Override
  public String key() {
    return indexedFile.key();
  }

  public String moduleKey() {
    return indexedFile.moduleKey();
  }

  @Override
  public int hashCode() {
    return indexedFile.hashCode();
  }

  @Override
  public String toString() {
    return indexedFile.toString();
  }

  /**
   * {@link #setStatus(org.sonar.api.batch.fs.InputFile.Status)}
   */
  @Override
  public Status status() {
    checkMetadata();
    return status;
  }

  @Override
  public int lines() {
    checkMetadata();
    return metadata.lines();
  }

  @Override
  public boolean isEmpty() {
    checkMetadata();
    return metadata.lastValidOffset() == 0;
  }

  @Override
  public Charset charset() {
    checkMetadata();
    return charset;
  }

  public int lastValidOffset() {
    checkMetadata();
    Preconditions.checkState(metadata.lastValidOffset() >= 0, "InputFile is not properly initialized.");
    return metadata.lastValidOffset();
  }

  /**
   * Digest hash of the file.
   */
  public String hash() {
    checkMetadata();
    return metadata.hash();
  }

  public int nonBlankLines() {
    checkMetadata();
    return metadata.nonBlankLines();
  }

  public int[] originalLineOffsets() {
    checkMetadata();
    Preconditions.checkState(metadata.originalLineOffsets() != null, "InputFile is not properly initialized.");
    Preconditions.checkState(metadata.originalLineOffsets().length == metadata.lines(),
      "InputFile is not properly initialized. 'originalLineOffsets' property length should be equal to 'lines'");
    return metadata.originalLineOffsets();
  }

  @Override
  public TextPointer newPointer(int line, int lineOffset) {
    checkMetadata();
    DefaultTextPointer textPointer = new DefaultTextPointer(line, lineOffset);
    checkValid(textPointer, "pointer");
    return textPointer;
  }

  @Override
  public TextRange newRange(TextPointer start, TextPointer end) {
    checkMetadata();
    checkValid(start, "start pointer");
    checkValid(end, "end pointer");
    return newRangeValidPointers(start, end, false);
  }

  @Override
  public TextRange newRange(int startLine, int startLineOffset, int endLine, int endLineOffset) {
    checkMetadata();
    TextPointer start = newPointer(startLine, startLineOffset);
    TextPointer end = newPointer(endLine, endLineOffset);
    return newRangeValidPointers(start, end, false);
  }

  @Override
  public TextRange selectLine(int line) {
    checkMetadata();
    TextPointer startPointer = newPointer(line, 0);
    TextPointer endPointer = newPointer(line, lineLength(line));
    return newRangeValidPointers(startPointer, endPointer, true);
  }

  public void validate(TextRange range) {
    checkMetadata();
    checkValid(range.start(), "start pointer");
    checkValid(range.end(), "end pointer");
  }

  /**
   * Create Range from global offsets. Used for backward compatibility with older API.
   */
  public TextRange newRange(int startOffset, int endOffset) {
    checkMetadata();
    return newRangeValidPointers(newPointer(startOffset), newPointer(endOffset), false);
  }

  public TextPointer newPointer(int globalOffset) {
    checkMetadata();
    Preconditions.checkArgument(globalOffset >= 0, "%s is not a valid offset for a file", globalOffset);
    Preconditions.checkArgument(globalOffset <= lastValidOffset(), "%s is not a valid offset for file %s. Max offset is %s", globalOffset, this, lastValidOffset());
    int line = findLine(globalOffset);
    int startLineOffset = originalLineOffsets()[line - 1];
    return new DefaultTextPointer(line, globalOffset - startLineOffset);
  }

  public DefaultInputFile setStatus(Status status) {
    this.status = status;
    return this;
  }

  public DefaultInputFile setCharset(Charset charset) {
    this.charset = charset;
    return this;
  }

  private void checkValid(TextPointer pointer, String owner) {
    Preconditions.checkArgument(pointer.line() >= 1, "%s is not a valid line for a file", pointer.line());
    Preconditions.checkArgument(pointer.line() <= this.metadata.lines(), "%s is not a valid line for %s. File %s has %s line(s)", pointer.line(), owner, this, metadata.lines());
    Preconditions.checkArgument(pointer.lineOffset() >= 0, "%s is not a valid line offset for a file", pointer.lineOffset());
    int lineLength = lineLength(pointer.line());
    Preconditions.checkArgument(pointer.lineOffset() <= lineLength,
      "%s is not a valid line offset for %s. File %s has %s character(s) at line %s", pointer.lineOffset(), owner, this, lineLength, pointer.line());
  }

  private int lineLength(int line) {
    return lastValidGlobalOffsetForLine(line) - originalLineOffsets()[line - 1];
  }

  private int lastValidGlobalOffsetForLine(int line) {
    return line < this.metadata.lines() ? (originalLineOffsets()[line] - 1) : lastValidOffset();
  }

  private static TextRange newRangeValidPointers(TextPointer start, TextPointer end, boolean acceptEmptyRange) {
    Preconditions.checkArgument(acceptEmptyRange ? (start.compareTo(end) <= 0) : (start.compareTo(end) < 0),
      "Start pointer %s should be before end pointer %s", start, end);
    return new DefaultTextRange(start, end);
  }

  private int findLine(int globalOffset) {
    return Math.abs(Arrays.binarySearch(originalLineOffsets(), globalOffset) + 1);
  }

  private DefaultInputFile setMetadata(Metadata metadata) {
    this.metadata = metadata;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    // Use instanceof to support DeprecatedDefaultInputFile
    if (!(o instanceof DefaultInputFile)) {
      return false;
    }

    DefaultInputFile that = (DefaultInputFile) o;
    return this.moduleKey().equals(that.moduleKey()) && this.relativePath().equals(that.relativePath());
  }

  @Override
  public boolean isFile() {
    return true;
  }

}
