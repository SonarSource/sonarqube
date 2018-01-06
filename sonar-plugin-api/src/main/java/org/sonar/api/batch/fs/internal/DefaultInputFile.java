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
package org.sonar.api.batch.fs.internal;

import com.google.common.base.Preconditions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;

/**
 * @since 4.2
 * To create {@link InputFile} in tests, use {@link TestInputFileBuilder}.
 */
public class DefaultInputFile extends DefaultInputComponent implements InputFile {

  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

  private final DefaultIndexedFile indexedFile;
  private final String contents;
  private final Consumer<DefaultInputFile> metadataGenerator;

  private Status status;
  private Charset charset;
  private Metadata metadata;
  private boolean published;
  private boolean excludedForCoverage;

  public DefaultInputFile(DefaultIndexedFile indexedFile, Consumer<DefaultInputFile> metadataGenerator) {
    this(indexedFile, metadataGenerator, null);
  }

  // For testing
  public DefaultInputFile(DefaultIndexedFile indexedFile, Consumer<DefaultInputFile> metadataGenerator, @Nullable String contents) {
    super(indexedFile.batchId());
    this.indexedFile = indexedFile;
    this.metadataGenerator = metadataGenerator;
    this.metadata = null;
    this.published = false;
    this.excludedForCoverage = false;
    this.contents = contents;
  }

  public void checkMetadata() {
    if (metadata == null) {
      metadataGenerator.accept(this);
    }
  }

  @Override
  public InputStream inputStream() throws IOException {
    return contents != null ? new ByteArrayInputStream(contents.getBytes(charset()))
      : new BOMInputStream(Files.newInputStream(path()),
        ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE);
  }

  @Override
  public String contents() throws IOException {
    if (contents != null) {
      return contents;
    } else {
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      try (InputStream inputStream = inputStream()) {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
          result.write(buffer, 0, length);
        }
      }
      return result.toString(charset().name());
    }
  }

  public DefaultInputFile setPublished(boolean published) {
    this.published = published;
    return this;
  }

  public boolean isPublished() {
    return published;
  }

  public DefaultInputFile setExcludedForCoverage(boolean excludedForCoverage) {
    this.excludedForCoverage = excludedForCoverage;
    return this;
  }

  public boolean isExcludedForCoverage() {
    return excludedForCoverage;
  }

  /**
   * @deprecated since 6.6
   */
  @Deprecated
  @Override
  public String relativePath() {
    return indexedFile.relativePath();
  }

  public String getModuleRelativePath() {
    return indexedFile.getModuleRelativePath();
  }

  public String getProjectRelativePath() {
    return indexedFile.getProjectRelativePath();
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
   * Component key (without branch).
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

  public DefaultInputFile setMetadata(Metadata metadata) {
    this.metadata = metadata;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (this.getClass() != obj.getClass()) {
      return false;
    }

    DefaultInputFile that = (DefaultInputFile) obj;
    return this.getProjectRelativePath().equals(that.getProjectRelativePath());
  }

  @Override
  public boolean isFile() {
    return true;
  }

  @Override
  public String filename() {
    return indexedFile.filename();
  }

  @Override
  public URI uri() {
    return indexedFile.uri();
  }

}
