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

import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.PathUtils;

public class TestInputFileBuilder {
  public static int batchId = 1;

  private final int id;
  private final String relativePath;
  private final String moduleKey;
  private Path moduleBaseDir;
  private String language;
  private InputFile.Type type = InputFile.Type.MAIN;
  private InputFile.Status status;
  private int lines = -1;
  private Charset charset;
  private int lastValidOffset = -1;
  private String hash;
  private int nonBlankLines;
  private int[] originalLineOffsets;

  public TestInputFileBuilder(String moduleKey, String relativePath) {
    this(moduleKey, relativePath, batchId++);
  }

  public TestInputFileBuilder(String moduleKey, String relativePath, int id) {
    this.moduleKey = moduleKey;
    this.moduleBaseDir = Paths.get(moduleKey);
    this.relativePath = PathUtils.sanitize(relativePath);
    this.id = id;
  }

  public TestInputFileBuilder setModuleBaseDir(Path moduleBaseDir) {
    this.moduleBaseDir = moduleBaseDir.normalize();
    return this;
  }

  public TestInputFileBuilder setLanguage(@Nullable String language) {
    this.language = language;
    return this;
  }

  public TestInputFileBuilder setType(InputFile.Type type) {
    this.type = type;
    return this;
  }

  public TestInputFileBuilder setStatus(InputFile.Status status) {
    this.status = status;
    return this;
  }

  public TestInputFileBuilder setLines(int lines) {
    this.lines = lines;
    return this;
  }

  public TestInputFileBuilder setCharset(Charset charset) {
    this.charset = charset;
    return this;
  }

  public TestInputFileBuilder setLastValidOffset(int lastValidOffset) {
    this.lastValidOffset = lastValidOffset;
    return this;
  }

  public TestInputFileBuilder setHash(String hash) {
    this.hash = hash;
    return this;
  }

  public TestInputFileBuilder setNonBlankLines(int nonBlankLines) {
    this.nonBlankLines = nonBlankLines;
    return this;
  }

  public TestInputFileBuilder setOriginalLineOffsets(int[] originalLineOffsets) {
    this.originalLineOffsets = originalLineOffsets;
    return this;
  }

  public TestInputFileBuilder setMetadata(Metadata metadata) {
    this.setLines(metadata.lines());
    this.setLastValidOffset(metadata.lastValidOffset());
    this.setNonBlankLines(metadata.nonBlankLines());
    this.setHash(metadata.hash());
    this.setOriginalLineOffsets(metadata.originalLineOffsets());
    return this;
  }

  public TestInputFileBuilder initMetadata(String content) {
    return setMetadata(new FileMetadata().readMetadata(new StringReader(content)));
  }

  public DefaultInputFile build() {
    DefaultIndexedFile indexedFile = new DefaultIndexedFile(moduleKey, moduleBaseDir, relativePath, type, id);
    indexedFile.setLanguage(language);
    DefaultInputFile inputFile = new DefaultInputFile(indexedFile, f -> new Metadata(lines, nonBlankLines, hash, originalLineOffsets, lastValidOffset));
    inputFile.setStatus(status);
    inputFile.setCharset(charset);
    return inputFile;
  }
}
