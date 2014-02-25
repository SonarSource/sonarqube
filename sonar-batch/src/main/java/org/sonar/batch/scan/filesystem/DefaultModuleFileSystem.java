/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.scan.filesystem;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.SonarException;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class can't be immutable because of execution of maven plugins that can change the project structure (see MavenPluginHandler and sonar.phase)
 *
 * @since 3.5
 */
public class DefaultModuleFileSystem extends DefaultFileSystem implements ModuleFileSystem {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultModuleFileSystem.class);

  private final String moduleKey;
  private final FileIndexer indexer;
  private final Settings settings;

  private File buildDir;
  private List<File> sourceDirs = Lists.newArrayList();
  private List<File> testDirs = Lists.newArrayList();
  private List<File> binaryDirs = Lists.newArrayList();
  private List<File> sourceFiles = Lists.newArrayList();
  private List<File> testFiles = Lists.newArrayList();
  private ComponentIndexer componentIndexer;
  private boolean initialized;

  public DefaultModuleFileSystem(ModuleInputFileCache moduleInputFileCache, Project module, Settings settings, FileIndexer indexer, ModuleFileSystemInitializer initializer,
    ComponentIndexer componentIndexer) {
    super(moduleInputFileCache);
    this.componentIndexer = componentIndexer;
    this.moduleKey = module.getKey();
    this.settings = settings;
    this.indexer = indexer;
    if (initializer.baseDir() != null) {
      setBaseDir(initializer.baseDir());
    }
    setWorkDir(initializer.workingDir());
    this.buildDir = initializer.buildDir();
    this.sourceDirs = initializer.sourceDirs();
    this.testDirs = initializer.testDirs();
    this.binaryDirs = initializer.binaryDirs();
    this.sourceFiles = initializer.additionalSourceFiles();
    this.testFiles = initializer.additionalTestFiles();
  }

  public boolean isInitialized() {
    return initialized;
  }

  public String moduleKey() {
    return moduleKey;
  }

  @Override
  @CheckForNull
  public File buildDir() {
    return buildDir;
  }

  @Override
  public List<File> sourceDirs() {
    if (sourceDirs.isEmpty()) {
      // For backward compatibility with File::fromIOFile(file, sourceDirs) we need to always return something
      return Arrays.asList(baseDir());
    }
    return sourceDirs;
  }

  @Override
  public List<File> testDirs() {
    return testDirs;
  }

  @Override
  public List<File> binaryDirs() {
    return binaryDirs;
  }

  List<File> sourceFiles() {
    return sourceFiles;
  }

  List<File> testFiles() {
    return testFiles;
  }

  @Override
  public Charset encoding() {
    final Charset charset;
    String encoding = settings.getString(CoreProperties.ENCODING_PROPERTY);
    if (StringUtils.isNotEmpty(encoding)) {
      charset = Charset.forName(StringUtils.trim(encoding));
    } else {
      charset = Charset.defaultCharset();
    }
    return charset;
  }

  @Override
  public boolean isDefaultJvmEncoding() {
    return !settings.hasKey(CoreProperties.ENCODING_PROPERTY);
  }

  /**
   * Should not be used - only for old plugins
   * @deprecated since 4.0
   */
  @Deprecated
  void addSourceDir(File dir) {
    throw new UnsupportedOperationException("Modifications of the file system are not permitted");
  }

  /**
   * Should not be used - only for old plugins
   * @deprecated since 4.0
   */
  @Deprecated
  void addTestDir(File dir) {
    throw new UnsupportedOperationException("Modifications of the file system are not permitted");
  }

  /**
   * @return
   * @deprecated in 4.2. Replaced by {@link #encoding()}
   */
  @Override
  @Deprecated
  public Charset sourceCharset() {
    return encoding();
  }

  /**
   * @deprecated in 4.2. Replaced by {@link #workDir()}
   */
  @Deprecated
  @Override
  public File workingDir() {
    return workDir();
  }

  @Override
  public List<File> files(FileQuery query) {
    doPreloadFiles();
    Collection<FilePredicate> predicates = Lists.newArrayList();
    for (Map.Entry<String, Collection<String>> entry : query.attributes().entrySet()) {
      predicates.add(fromDeprecatedAttribute(entry.getKey(), entry.getValue()));
    }
    return ImmutableList.copyOf(files(FilePredicates.and(predicates)));
  }

  @Override
  protected void doPreloadFiles() {
    if (!initialized) {
      LOG.warn("Accessing the filesystem before the Sensor phase is deprecated and will not be supported in the future. Please update your plugin.");
      indexer.index(this);
    }
  }

  public void resetDirs(File basedir, File buildDir, List<File> sourceDirs, List<File> testDirs, List<File> binaryDirs) {
    if (initialized) {
      throw new SonarException("Module filesystem is already initialized. Modifications of filesystem are only allowed during Initializer phase.");
    }
    setBaseDir(basedir);
    this.buildDir = buildDir;
    this.sourceDirs = existingDirs(sourceDirs);
    this.testDirs = existingDirs(testDirs);
    this.binaryDirs = existingDirs(binaryDirs);
  }

  public void index() {
    if (initialized) {
      throw new SonarException("Module filesystem can only be indexed once");
    }
    initialized = true;
    indexer.index(this);
    componentIndexer.execute(this);
  }

  private List<File> existingDirs(List<File> dirs) {
    ImmutableList.Builder<File> builder = ImmutableList.builder();
    for (File dir : dirs) {
      if (dir.exists() && dir.isDirectory()) {
        builder.add(dir);
      }
    }
    return builder.build();
  }

  static FilePredicate fromDeprecatedAttribute(String key, Collection<String> value) {
    if ("TYPE".equals(key)) {
      return FilePredicates.or(Collections2.transform(value, new Function<String, FilePredicate>() {
        @Override
        public FilePredicate apply(@Nullable String s) {
          return s == null ? FilePredicates.all() : FilePredicates.hasType(org.sonar.api.batch.fs.InputFile.Type.valueOf(s));
        }
      }));
    }
    if ("STATUS".equals(key)) {
      return FilePredicates.or(Collections2.transform(value, new Function<String, FilePredicate>() {
        @Override
        public FilePredicate apply(@Nullable String s) {
          return s == null ? FilePredicates.all() : FilePredicates.hasStatus(org.sonar.api.batch.fs.InputFile.Status.valueOf(s));
        }
      }));
    }
    if ("LANG".equals(key)) {
      return FilePredicates.or(Collections2.transform(value, new Function<String, FilePredicate>() {
        @Override
        public FilePredicate apply(@Nullable String s) {
          return s == null ? FilePredicates.all() : FilePredicates.hasLanguage(s);
        }
      }));
    }
    if ("CMP_KEY".equals(key)) {
      return FilePredicates.or(Collections2.transform(value, new Function<String, FilePredicate>() {
        @Override
        public FilePredicate apply(@Nullable String s) {
          return s == null ? FilePredicates.all() : new FilePredicateAdapters.KeyPredicate(s);
        }
      }));
    }
    if ("CMP_DEPRECATED_KEY".equals(key)) {
      return FilePredicates.or(Collections2.transform(value, new Function<String, FilePredicate>() {
        @Override
        public FilePredicate apply(@Nullable String s) {
          return s == null ? FilePredicates.all() : new FilePredicateAdapters.DeprecatedKeyPredicate(s);
        }
      }));
    }
    if ("SRC_REL_PATH".equals(key)) {
      return FilePredicates.or(Collections2.transform(value, new Function<String, FilePredicate>() {
        @Override
        public FilePredicate apply(@Nullable String s) {
          return s == null ? FilePredicates.all() : new FilePredicateAdapters.SourceRelativePathPredicate(s);
        }
      }));
    }
    if ("SRC_DIR_PATH".equals(key)) {
      return FilePredicates.or(Collections2.transform(value, new Function<String, FilePredicate>() {
        @Override
        public FilePredicate apply(@Nullable String s) {
          return s == null ? FilePredicates.all() : new FilePredicateAdapters.SourceDirPredicate(s);
        }
      }));
    }
    throw new IllegalArgumentException("Unsupported file attribute: " + key);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultModuleFileSystem that = (DefaultModuleFileSystem) o;
    return moduleKey.equals(that.moduleKey);
  }

  @Override
  public int hashCode() {
    return moduleKey.hashCode();
  }
}
