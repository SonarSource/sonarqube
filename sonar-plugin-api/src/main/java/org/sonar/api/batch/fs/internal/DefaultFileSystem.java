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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.UniqueIndexPredicate;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedSet;

/**
 * @since 4.2
 */
public class DefaultFileSystem implements FileSystem {

  private final Cache cache;
  private final SortedSet<String> languages = Sets.newTreeSet();
  private File baseDir, workDir;
  private Charset encoding;

  /**
   * Only for testing
   */
  public DefaultFileSystem() {
    this.cache = new MapCache();
  }

  protected DefaultFileSystem(Cache cache) {
    this.cache = cache;
  }

  public DefaultFileSystem setBaseDir(File d) {
    Preconditions.checkNotNull(d, "Base directory can't be null");
    this.baseDir = d.getAbsoluteFile();
    return this;
  }

  @Override
  public File baseDir() {
    return baseDir;
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
    this.workDir = d.getAbsoluteFile();
    return this;
  }

  @Override
  public File workDir() {
    return workDir;
  }

  @Override
  public InputFile inputFile(FilePredicate predicate) {
    doPreloadFiles();
    if (predicate instanceof UniqueIndexPredicate) {
      return cache.inputFile((UniqueIndexPredicate) predicate);
    }
    try {
      Iterable<InputFile> files = inputFiles(predicate);
      return Iterables.getOnlyElement(files);
    } catch (NoSuchElementException e) {
      // contrary to guava, return null if iterable is empty
      return null;
    }
  }

  @Override
  public Iterable<InputFile> inputFiles(FilePredicate predicate) {
    doPreloadFiles();
    return Iterables.filter(cache.inputFiles(), new GuavaPredicate(predicate));
  }

  @Override
  public boolean hasFiles(FilePredicate predicate) {
    doPreloadFiles();
    return Iterables.indexOf(cache.inputFiles(), new GuavaPredicate(predicate)) >= 0;
  }

  @Override
  public Iterable<File> files(FilePredicate predicate) {
    doPreloadFiles();
    return Iterables.transform(inputFiles(predicate), new Function<InputFile, File>() {
      @Override
      public File apply(@Nullable InputFile input) {
        return input == null ? null : input.file();
      }
    });
  }

  /**
   * Adds InputFile to the list and registers its language, if present.
   */
  public DefaultFileSystem add(InputFile inputFile) {
    cache.add(inputFile);
    if (inputFile.language() != null) {
      languages.add(inputFile.language());
    }
    return this;
  }

  /**
   * Adds a language to the list. To be used only for unit tests that need to use {@link #languages()} without
   * using {@link #add(org.sonar.api.batch.fs.InputFile)}.
   */
  public DefaultFileSystem addLanguages(String language, String... others) {
    languages.add(language);
    for (String other : others) {
      languages.add(other);
    }
    return this;
  }

  @Override
  public SortedSet<String> languages() {
    doPreloadFiles();
    return languages;
  }

  /**
   * This method is called before each search of files.
   */
  protected void doPreloadFiles() {
    // nothing to do by default
  }

  public static abstract class Cache {
    protected abstract Iterable<InputFile> inputFiles();

    @CheckForNull
    protected abstract InputFile inputFile(UniqueIndexPredicate predicate);

    protected abstract void doAdd(InputFile inputFile);

    protected abstract void doIndex(String indexId, Object value, InputFile inputFile);

    final void add(InputFile inputFile) {
      doAdd(inputFile);
      for (FileIndex index : FileIndex.ALL) {
        doIndex(index.id(), index.valueOf(inputFile), inputFile);
      }
    }
  }

  /**
   * Used only for testing
   */
  private static class MapCache extends Cache {
    private final List<InputFile> files = Lists.newArrayList();
    private final Map<String, Map<Object, InputFile>> fileMap = Maps.newHashMap();

    @Override
    public Iterable<InputFile> inputFiles() {
      return Lists.newArrayList(files);
    }

    @Override
    public InputFile inputFile(UniqueIndexPredicate predicate) {
      Map<Object, InputFile> byAttr = fileMap.get(predicate.indexId());
      if (byAttr != null) {
        return byAttr.get(predicate.value());
      }
      return null;
    }

    @Override
    protected void doAdd(InputFile inputFile) {
      files.add(inputFile);
    }

    @Override
    protected void doIndex(String indexId, Object value, InputFile inputFile) {
      Map<Object, InputFile> attrValues = fileMap.get(indexId);
      if (attrValues == null) {
        attrValues = Maps.newHashMap();
        fileMap.put(indexId, attrValues);
      }
      attrValues.put(value, inputFile);
    }
  }

  private static class GuavaPredicate implements Predicate<InputFile> {
    private final FilePredicate predicate;

    private GuavaPredicate(FilePredicate predicate) {
      this.predicate = predicate;
    }

    @Override
    public boolean apply(@Nullable InputFile input) {
      return input != null && predicate.apply(input);
    }
  }
}
