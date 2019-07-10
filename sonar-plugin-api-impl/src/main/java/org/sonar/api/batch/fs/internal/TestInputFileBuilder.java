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
package org.sonar.api.batch.fs.internal;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.PathUtils;

/**
 * Intended to be used in unit tests that need to create {@link InputFile}s.
 * An InputFile is unambiguously identified by a <b>module key</b> and a <b>relative path</b>, so these parameters are mandatory.
 * <p>
 * A module base directory is only needed to construct absolute paths.
 * <p>
 * Examples of usage of the constructors:
 *
 * <pre>
 * InputFile file1 = TestInputFileBuilder.create("module1", "myfile.java").build();
 * InputFile file2 = TestInputFileBuilder.create("", fs.baseDir(), myfile).build();
 * </pre>
 * <p>
 * file1 will have the "module1" as both module key and module base directory.
 * file2 has an empty string as module key, and a relative path which is the path from the filesystem base directory to myfile.
 *
 * @since 6.3
 */
public class TestInputFileBuilder {
  private static int batchId = 1;

  private final int id;
  private final String relativePath;
  private final String projectKey;
  @CheckForNull
  private Path projectBaseDir;
  private Path moduleBaseDir;
  private String language;
  private InputFile.Type type = InputFile.Type.MAIN;
  private InputFile.Status status;
  private int lines = -1;
  private Charset charset;
  private String hash;
  private int nonBlankLines;
  private int[] originalLineStartOffsets = new int[0];
  private int[] originalLineEndOffsets = new int[0];
  private int lastValidOffset = -1;
  private boolean publish = true;
  private String contents;

  /**
   * Create a InputFile identified by the given project key and relative path.
   */
  public TestInputFileBuilder(String projectKey, String relativePath) {
    this(projectKey, relativePath, batchId++);
  }

  /**
   * Create a InputFile with a given module key and module base directory.
   * The relative path is generated comparing the file path to the module base directory.
   * filePath must point to a file that is within the module base directory.
   */
  public TestInputFileBuilder(String projectKey, File moduleBaseDir, File filePath) {
    String relativePathStr = moduleBaseDir.toPath().relativize(filePath.toPath()).toString();
    this.projectKey = projectKey;
    setModuleBaseDir(moduleBaseDir.toPath());
    this.relativePath = PathUtils.sanitize(relativePathStr);
    this.id = batchId++;
  }

  public TestInputFileBuilder(String projectKey, String relativePath, int id) {
    this.projectKey = projectKey;
    setModuleBaseDir(Paths.get(projectKey));
    this.relativePath = PathUtils.sanitize(relativePath);
    this.id = id;
  }

  public static TestInputFileBuilder create(String moduleKey, File moduleBaseDir, File filePath) {
    return new TestInputFileBuilder(moduleKey, moduleBaseDir, filePath);
  }

  public static TestInputFileBuilder create(String moduleKey, String relativePath) {
    return new TestInputFileBuilder(moduleKey, relativePath);
  }

  public static int nextBatchId() {
    return batchId++;
  }

  public TestInputFileBuilder setProjectBaseDir(Path projectBaseDir) {
    this.projectBaseDir = normalize(projectBaseDir);
    return this;
  }

  public TestInputFileBuilder setModuleBaseDir(Path moduleBaseDir) {
    this.moduleBaseDir = normalize(moduleBaseDir);
    return this;
  }

  private static Path normalize(Path path) {
    try {
      return path.normalize().toRealPath(LinkOption.NOFOLLOW_LINKS);
    } catch (IOException e) {
      return path.normalize();
    }
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

  public TestInputFileBuilder setHash(String hash) {
    this.hash = hash;
    return this;
  }

  /**
   * Set contents of the file and calculates metadata from it.
   * The contents will be returned by {@link InputFile#contents()} and {@link InputFile#inputStream()} and can be
   * inconsistent with the actual physical file pointed by {@link InputFile#path()}, {@link InputFile#absolutePath()}, etc.
   */
  public TestInputFileBuilder setContents(String content) {
    this.contents = content;
    initMetadata(content);
    return this;
  }

  public TestInputFileBuilder setNonBlankLines(int nonBlankLines) {
    this.nonBlankLines = nonBlankLines;
    return this;
  }

  public TestInputFileBuilder setLastValidOffset(int lastValidOffset) {
    this.lastValidOffset = lastValidOffset;
    return this;
  }

  public TestInputFileBuilder setOriginalLineStartOffsets(int[] originalLineStartOffsets) {
    this.originalLineStartOffsets = originalLineStartOffsets;
    return this;
  }

  public TestInputFileBuilder setOriginalLineEndOffsets(int[] originalLineEndOffsets) {
    this.originalLineEndOffsets = originalLineEndOffsets;
    return this;
  }

  public TestInputFileBuilder setPublish(boolean publish) {
    this.publish = publish;
    return this;
  }

  public TestInputFileBuilder setMetadata(Metadata metadata) {
    this.setLines(metadata.lines());
    this.setLastValidOffset(metadata.lastValidOffset());
    this.setNonBlankLines(metadata.nonBlankLines());
    this.setHash(metadata.hash());
    this.setOriginalLineStartOffsets(metadata.originalLineStartOffsets());
    this.setOriginalLineEndOffsets(metadata.originalLineEndOffsets());
    return this;
  }

  public TestInputFileBuilder initMetadata(String content) {
    return setMetadata(new FileMetadata().readMetadata(new StringReader(content)));
  }

  public DefaultInputFile build() {
    Path absolutePath = moduleBaseDir.resolve(relativePath);
    if (projectBaseDir == null) {
      projectBaseDir = moduleBaseDir;
    }
    String projectRelativePath = projectBaseDir.relativize(absolutePath).toString();
    DefaultIndexedFile indexedFile = new DefaultIndexedFile(absolutePath, projectKey, projectRelativePath, relativePath, type, language, id, new SensorStrategy());
    DefaultInputFile inputFile = new DefaultInputFile(indexedFile,
      f -> f.setMetadata(new Metadata(lines, nonBlankLines, hash, originalLineStartOffsets, originalLineEndOffsets, lastValidOffset)),
      contents);
    inputFile.setStatus(status);
    inputFile.setCharset(charset);
    inputFile.setPublished(publish);
    return inputFile;
  }

  public static DefaultInputModule newDefaultInputModule(String moduleKey, File baseDir) {
    ProjectDefinition definition = ProjectDefinition.create()
      .setKey(moduleKey)
      .setBaseDir(baseDir)
      .setWorkDir(new File(baseDir, ".sonar"));
    return newDefaultInputModule(definition);
  }

  public static DefaultInputModule newDefaultInputModule(ProjectDefinition projectDefinition) {
    return new DefaultInputModule(projectDefinition, TestInputFileBuilder.nextBatchId());
  }

  public static DefaultInputModule newDefaultInputModule(AbstractProjectOrModule parent, String key) throws IOException {
    Path basedir = parent.getBaseDir().resolve(key);
    Files.createDirectory(basedir);
    return newDefaultInputModule(key, basedir.toFile());
  }

  public static DefaultInputProject newDefaultInputProject(String projectKey, File baseDir) {
    ProjectDefinition definition = ProjectDefinition.create()
      .setKey(projectKey)
      .setBaseDir(baseDir)
      .setWorkDir(new File(baseDir, ".sonar"));
    return newDefaultInputProject(definition);
  }

  public static DefaultInputProject newDefaultInputProject(ProjectDefinition projectDefinition) {
    return new DefaultInputProject(projectDefinition, TestInputFileBuilder.nextBatchId());
  }

  public static DefaultInputProject newDefaultInputProject(String key, Path baseDir) throws IOException {
    Files.createDirectory(baseDir);
    return newDefaultInputProject(key, baseDir.toFile());
  }

  public static DefaultInputDir newDefaultInputDir(AbstractProjectOrModule module, String relativePath) throws IOException {
    Path basedir = module.getBaseDir().resolve(relativePath);
    Files.createDirectory(basedir);
    return new DefaultInputDir(module.key(), relativePath)
      .setModuleBaseDir(module.getBaseDir());
  }

  public static DefaultInputFile newDefaultInputFile(Path projectBaseDir, AbstractProjectOrModule module, String relativePath) {
    return new TestInputFileBuilder(module.key(), relativePath)
      .setStatus(InputFile.Status.SAME)
      .setProjectBaseDir(projectBaseDir)
      .setModuleBaseDir(module.getBaseDir())
      .build();
  }
}
