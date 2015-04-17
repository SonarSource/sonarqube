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
package org.sonar.api.batch.fs.internal;

import com.google.common.base.Preconditions;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.FileMetadata.Metadata;
import org.sonar.api.utils.PathUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * @since 4.2
 */
public class DefaultInputFile implements InputFile {

  private final String relativePath;
  private final String moduleKey;
  protected Path moduleBaseDir;
  private String language;
  private Type type = Type.MAIN;
  private Status status;
  private int lines = -1;
  private Charset charset;
  private int lastValidOffset = -1;
  private String hash;
  private int nonBlankLines;
  private int[] originalLineOffsets;

  public DefaultInputFile(String moduleKey, String relativePath) {
    this.moduleKey = moduleKey;
    this.relativePath = PathUtils.sanitize(relativePath);
  }

  @Override
  public String relativePath() {
    return relativePath;
  }

  @Override
  public String absolutePath() {
    return PathUtils.sanitize(path().toString());
  }

  @Override
  public File file() {
    return path().toFile();
  }

  @Override
  public Path path() {
    if (moduleBaseDir == null) {
      throw new IllegalStateException("Can not return the java.nio.file.Path because module baseDir is not set (see method setModuleBaseDir(java.io.File))");
    }
    return moduleBaseDir.resolve(relativePath);
  }

  @CheckForNull
  @Override
  public String language() {
    return language;
  }

  @Override
  public Type type() {
    return type;
  }

  /**
   * {@link #setStatus(org.sonar.api.batch.fs.InputFile.Status)}
   */
  @Override
  public Status status() {
    return status;
  }

  @Override
  public int lines() {
    return lines;
  }

  @Override
  public boolean isEmpty() {
    return lastValidOffset == 0;
  }

  /**
   * Component key.
   */
  public String key() {
    return new StringBuilder().append(moduleKey).append(":").append(relativePath).toString();
  }

  public String moduleKey() {
    return moduleKey;
  }

  public Charset charset() {
    return charset;
  }

  /**
   * For testing purpose. Will be automaticall set when file is added to {@link DefaultFileSystem}
   */
  public DefaultInputFile setModuleBaseDir(Path moduleBaseDir) {
    this.moduleBaseDir = moduleBaseDir.normalize();
    return this;
  }

  public DefaultInputFile setLanguage(@Nullable String language) {
    this.language = language;
    return this;
  }

  public DefaultInputFile setType(Type type) {
    this.type = type;
    return this;
  }

  public DefaultInputFile setStatus(Status status) {
    this.status = status;
    return this;
  }

  public DefaultInputFile setLines(int lines) {
    this.lines = lines;
    return this;
  }

  public DefaultInputFile setCharset(Charset charset) {
    this.charset = charset;
    return this;
  }

  public int lastValidOffset() {
    Preconditions.checkState(lastValidOffset >= 0, "InputFile is not properly initialized. Please set 'lastValidOffset' property.");
    return lastValidOffset;
  }

  public DefaultInputFile setLastValidOffset(int lastValidOffset) {
    this.lastValidOffset = lastValidOffset;
    return this;
  }

  /**
   * Digest hash of the file.
   */
  public String hash() {
    return hash;
  }

  public int nonBlankLines() {
    return nonBlankLines;
  }

  public int[] originalLineOffsets() {
    Preconditions.checkState(originalLineOffsets != null, "InputFile is not properly initialized. Please set 'originalLineOffsets' property.");
    Preconditions.checkState(originalLineOffsets.length == lines, "InputFile is not properly initialized. 'originalLineOffsets' property length should be equal to 'lines'");
    return originalLineOffsets;
  }

  public DefaultInputFile setHash(String hash) {
    this.hash = hash;
    return this;
  }

  public DefaultInputFile setNonBlankLines(int nonBlankLines) {
    this.nonBlankLines = nonBlankLines;
    return this;
  }

  public DefaultInputFile setOriginalLineOffsets(int[] originalLineOffsets) {
    this.originalLineOffsets = originalLineOffsets;
    return this;
  }

  @Override
  public TextPointer newPointer(int line, int lineOffset) {
    DefaultTextPointer textPointer = new DefaultTextPointer(line, lineOffset);
    checkValid(textPointer, "pointer");
    return textPointer;
  }

  private void checkValid(TextPointer pointer, String owner) {
    Preconditions.checkArgument(pointer.line() >= 1, "%s is not a valid line for a file", pointer.line());
    Preconditions.checkArgument(pointer.line() <= this.lines, "%s is not a valid line for %s. File %s has %s line(s)", pointer.line(), owner, this, lines);
    Preconditions.checkArgument(pointer.lineOffset() >= 0, "%s is not a valid line offset for a file", pointer.lineOffset());
    int lineLength = lineLength(pointer.line());
    Preconditions.checkArgument(pointer.lineOffset() <= lineLength,
      "%s is not a valid line offset for %s. File %s has %s character(s) at line %s", pointer.lineOffset(), owner, this, lineLength, pointer.line());
  }

  private int lineLength(int line) {
    return lastValidGlobalOffsetForLine(line) - originalLineOffsets()[line - 1];
  }

  private int lastValidGlobalOffsetForLine(int line) {
    return line < this.lines ? (originalLineOffsets()[line] - 1) : lastValidOffset();
  }

  @Override
  public TextRange newRange(TextPointer start, TextPointer end) {
    checkValid(start, "start pointer");
    checkValid(end, "end pointer");
    return newRangeValidPointers(start, end);
  }

  private TextRange newRangeValidPointers(TextPointer start, TextPointer end) {
    Preconditions.checkArgument(start.compareTo(end) < 0, "Start pointer %s should be before end pointer %s", start, end);
    return new DefaultTextRange(start, end);
  }

  /**
   * Create Range from global offsets. Used for backward compatibility with older API.
   */
  public TextRange newRange(int startOffset, int endOffset) {
    return newRangeValidPointers(newPointer(startOffset), newPointer(endOffset));
  }

  public TextPointer newPointer(int globalOffset) {
    Preconditions.checkArgument(globalOffset >= 0, "%s is not a valid offset for a file", globalOffset);
    Preconditions.checkArgument(globalOffset <= lastValidOffset(), "%s is not a valid offset for file %s. Max offset is %s", globalOffset, this, lastValidOffset());
    int line = findLine(globalOffset);
    int startLineOffset = originalLineOffsets()[line - 1];
    return new DefaultTextPointer(line, globalOffset - startLineOffset);
  }

  private int findLine(int globalOffset) {
    return Math.abs(Arrays.binarySearch(originalLineOffsets(), globalOffset) + 1);
  }

  public DefaultInputFile initMetadata(Metadata metadata) {
    this.setLines(metadata.lines);
    this.setLastValidOffset(metadata.lastValidOffset);
    this.setNonBlankLines(metadata.nonBlankLines);
    this.setHash(metadata.hash);
    this.setOriginalLineOffsets(metadata.originalLineOffsets);
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
    return moduleKey.equals(that.moduleKey) && relativePath.equals(that.relativePath);
  }

  @Override
  public int hashCode() {
    return moduleKey.hashCode() + relativePath.hashCode() * 13;
  }

  @Override
  public String toString() {
    return "[moduleKey=" + moduleKey + ", relative=" + relativePath + ", basedir=" + moduleBaseDir + "]";
  }

}
