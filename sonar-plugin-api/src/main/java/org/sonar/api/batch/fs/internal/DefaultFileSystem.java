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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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
  private final Path baseDir;
  private Path workDir;
  private Charset encoding;
  protected final FilePredicates predicates;
  private Function<FilePredicate, Predicate<InputFile>> defaultPredicateFactory;

  /**
   * Only for testing
   */
  public DefaultFileSystem(Path baseDir) {
    this(baseDir, new MapCache());
  }

  /**
   * Only for testing
   */
  public DefaultFileSystem(File baseDir) {
    this(baseDir.toPath(), new MapCache());
  }

  protected DefaultFileSystem(Path baseDir, Cache cache) {
    this.baseDir = baseDir;
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

  public DefaultFileSystem setEncoding(Charset e) {
    this.encoding = e;
    return this;
  }

  @Override
  public Charset encoding() {
    return encoding;
  }

  public DefaultFileSystem setWorkDir(Path d) {
    this.workDir = d;
    return this;
  }

  public DefaultFileSystem setDefaultPredicate(@Nullable Function<FilePredicate, Predicate<InputFile>> defaultPredicateFactory) {
    this.defaultPredicateFactory = defaultPredicateFactory;
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
    return OptimizedFilePredicateAdapter.create(predicates.all()).get(cache);
  }

  @Override
  public Iterable<InputFile> inputFiles(FilePredicate predicate) {
    Iterable<InputFile> iterable = OptimizedFilePredicateAdapter.create(predicate).get(cache);
    if (defaultPredicateFactory != null) {
      return StreamSupport.stream(iterable.spliterator(), false)
        .filter(defaultPredicateFactory.apply(predicate)).collect(Collectors.toList());
    }
    return iterable;
  }

  @Override
  public boolean hasFiles(FilePredicate predicate) {
    return inputFiles(predicate).iterator().hasNext();
  }

  @Override
  public Iterable<File> files(FilePredicate predicate) {
    return () -> StreamSupport.stream(inputFiles(predicate).spliterator(), false)
      .map(InputFile::file)
      .iterator();
  }

  @Override
  public InputDir inputDir(File dir) {
    String relativePath = PathUtils.sanitize(new PathResolver().relativePath(baseDir.toFile(), dir));
    if (relativePath == null) {
      return null;
    }
    return cache.inputDir(relativePath);
  }

  public DefaultFileSystem add(InputFile inputFile) {
    cache.add(inputFile);
    return this;
  }

  public DefaultFileSystem add(DefaultInputDir inputDir) {
    cache.add(inputDir);
    return this;
  }

  @Override
  public SortedSet<String> languages() {
    return cache.languages();
  }

  @Override
  public FilePredicates predicates() {
    return predicates;
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

    protected abstract SortedSet<String> languages();
  }

  /**
   * Used only for testing
   */
  private static class MapCache extends Cache {
    private final Map<String, InputFile> fileMap = new HashMap<>();
    private final Map<String, InputDir> dirMap = new HashMap<>();
    private final SetMultimap<String, InputFile> filesByNameCache = LinkedHashMultimap.create();
    private final SetMultimap<String, InputFile> filesByExtensionCache = LinkedHashMultimap.create();
    private SortedSet<String> languages = new TreeSet<>();

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
    public Iterable<InputFile> getFilesByName(String filename) {
      return filesByNameCache.get(filename);
    }

    @Override
    public Iterable<InputFile> getFilesByExtension(String extension) {
      return filesByExtensionCache.get(extension);
    }

    @Override
    protected void doAdd(InputFile inputFile) {
      if (inputFile.language() != null) {
        languages.add(inputFile.language());
      }
      fileMap.put(inputFile.relativePath(), inputFile);
      filesByNameCache.put(inputFile.filename(), inputFile);
      filesByExtensionCache.put(FileExtensionPredicate.getExtension(inputFile), inputFile);
    }

    @Override
    protected void doAdd(InputDir inputDir) {
      dirMap.put(inputDir.relativePath(), inputDir);
    }

    @Override
    protected SortedSet<String> languages() {
      return languages;
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
