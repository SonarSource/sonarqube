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

import com.google.common.collect.Iterables;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.PathUtils;

/**
 * @since 4.2
 */
public class DefaultFileSystem implements FileSystem {

  private final Cache cache;
  private final SortedSet<String> languages = new TreeSet<>();
  private final Path baseDir;
  private Path workDir;
  private Charset encoding;
  protected final FilePredicates predicates;
  private FilePredicate defaultPredicate;

  /**
   * Only for testing
   */
  public DefaultFileSystem(Path baseDir) {
    this(baseDir.toFile(), new MapCache());
  }

  /**
   * Only for testing
   */
  public DefaultFileSystem(File baseDir) {
    this(baseDir, new MapCache());
  }

  protected DefaultFileSystem(@Nullable File baseDir, Cache cache) {
    // Basedir can be null with views
    try {
      this.baseDir = baseDir != null ? baseDir.toPath().toRealPath(LinkOption.NOFOLLOW_LINKS) : new File(".").toPath().toAbsolutePath().normalize();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    this.cache = cache;
    this.predicates = new DefaultFilePredicates(this.baseDir);
  }

  public Path baseDirPath() {
    return baseDir;
  }

  @Override
  public File baseDir() {
    return baseDir.toFile();
  }

  public DefaultFileSystem setEncoding(@Nullable Charset e) {
    this.encoding = e;
    return this;
  }

  @Override
  public Charset encoding() {
    return encoding == null ? Charset.defaultCharset() : encoding;
  }

  public boolean isDefaultJvmEncoding() {
    return encoding == null;
  }

  public DefaultFileSystem setWorkDir(File d) {
    this.workDir = d.getAbsoluteFile().toPath().normalize();
    return this;
  }

  public DefaultFileSystem setDefaultPredicate(@Nullable FilePredicate predicate) {
    this.defaultPredicate = predicate;
    return this;
  }

  @Override
  public File workDir() {
    return workDir.toFile();
  }

  @Override
  public InputFile inputFile(FilePredicate predicate) {
    Iterable<InputFile> files = inputFiles(predicate);
    Iterator<InputFile> iterator = files.iterator();
    if (!iterator.hasNext()) {
      return null;
    }
    InputFile first = iterator.next();
    if (!iterator.hasNext()) {
      return first;
    }

    StringBuilder sb = new StringBuilder();
    sb.append("expected one element but was: <" + first);
    for (int i = 0; i < 4 && iterator.hasNext(); i++) {
      sb.append(", " + iterator.next());
    }
    if (iterator.hasNext()) {
      sb.append(", ...");
    }
    sb.append('>');

    throw new IllegalArgumentException(sb.toString());

  }

  /**
   * Bypass default predicate to get all files/dirs indexed.
   * Default predicate is used when some files/dirs should not be processed by sensors.
   */
  public Iterable<InputFile> inputFiles() {
    doPreloadFiles();
    return OptimizedFilePredicateAdapter.create(predicates.all()).get(cache);
  }

  @Override
  public Iterable<InputFile> inputFiles(FilePredicate predicate) {
    doPreloadFiles();
    FilePredicate combinedPredicate = predicate;
    if (defaultPredicate != null) {
      combinedPredicate = predicates().and(defaultPredicate, predicate);
    }
    return OptimizedFilePredicateAdapter.create(combinedPredicate).get(cache);
  }

  @Override
  public boolean hasFiles(FilePredicate predicate) {
    return inputFiles(predicate).iterator().hasNext();
  }

  @Override
  public Iterable<File> files(FilePredicate predicate) {
    doPreloadFiles();
    return Iterables.transform(inputFiles(predicate), InputFile::file);
  }

  @Override
  public InputDir inputDir(File dir) {
    doPreloadFiles();
    String relativePath = PathUtils.sanitize(new PathResolver().relativePath(baseDir.toFile(), dir));
    if (relativePath == null) {
      return null;
    }
    return cache.inputDir(relativePath);
  }

  /**
   * Adds InputFile to the list and registers its language, if present.
   * Synchronized because PersistIt Exchange is not concurrent
   */
  public synchronized DefaultFileSystem add(DefaultInputFile inputFile) {
    if (this.baseDir == null) {
      throw new IllegalStateException("Please set basedir on filesystem before adding files");
    }
    inputFile.setModuleBaseDir(this.baseDir);
    cache.add(inputFile);
    String language = inputFile.language();
    if (language != null) {
      languages.add(language);
    }
    return this;
  }

  /**
   * Adds InputDir to the list.
   * Synchronized because PersistIt Exchange is not concurrent
   */
  public synchronized DefaultFileSystem add(DefaultInputDir inputDir) {
    if (this.baseDir == null) {
      throw new IllegalStateException("Please set basedir on filesystem before adding dirs");
    }
    inputDir.setModuleBaseDir(this.baseDir);
    cache.add(inputDir);
    return this;
  }

  /**
   * Adds a language to the list. To be used only for unit tests that need to use {@link #languages()} without
   * using {@link #add(DefaultInputFile)}.
   */
  public DefaultFileSystem addLanguages(String language, String... others) {
    languages.add(language);
    Collections.addAll(languages, others);
    return this;
  }

  @Override
  public SortedSet<String> languages() {
    doPreloadFiles();
    return languages;
  }

  @Override
  public FilePredicates predicates() {
    return predicates;
  }

  /**
   * This method is called before each search of files.
   */
  protected void doPreloadFiles() {
    // nothing to do by default
  }

  public abstract static class Cache implements Index {

    protected abstract void doAdd(InputFile inputFile);

    protected abstract void doAdd(InputDir inputDir);

    final void add(InputFile inputFile) {
      doAdd(inputFile);
    }

    public void add(InputDir inputDir) {
      doAdd(inputDir);
    }

  }

  /**
   * Used only for testing
   */
  private static class MapCache extends Cache {
    private final Map<String, InputFile> fileMap = new HashMap<>();
    private final Map<String, InputDir> dirMap = new HashMap<>();

    @Override
    public Iterable<InputFile> inputFiles() {
      return new ArrayList<>(fileMap.values());
    }

    @Override
    public InputFile inputFile(String relativePath) {
      return fileMap.get(relativePath);
    }

    @Override
    public InputDir inputDir(String relativePath) {
      return dirMap.get(relativePath);
    }

    @Override
    protected void doAdd(InputFile inputFile) {
      fileMap.put(inputFile.relativePath(), inputFile);
    }

    @Override
    protected void doAdd(InputDir inputDir) {
      dirMap.put(inputDir.relativePath(), inputDir);
    }
  }

  @Override
  public File resolvePath(String path) {
    File file = new File(path);
    if (!file.isAbsolute()) {
      try {
        file = new File(baseDir(), path).getCanonicalFile();
      } catch (IOException e) {
        throw new IllegalArgumentException("Unable to resolve path '" + path + "'", e);
      }
    }
    return file;
  }
}
