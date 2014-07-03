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
package org.sonar.batch.scan.filesystem;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.MessageException;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
  private List<File> sourceDirsOrFiles = Lists.newArrayList();
  private List<File> testDirsOrFiles = Lists.newArrayList();
  private List<File> binaryDirs = Lists.newArrayList();
  private ComponentIndexer componentIndexer;
  private boolean initialized;

  /**
   * Used by scan2 
   */
  public DefaultModuleFileSystem(ModuleInputFileCache moduleInputFileCache, ProjectDefinition def, Settings settings,
    FileIndexer indexer, ModuleFileSystemInitializer initializer) {
    this(moduleInputFileCache, def.getKey(), settings, indexer, initializer, null);
  }

  public DefaultModuleFileSystem(ModuleInputFileCache moduleInputFileCache, ProjectDefinition def, Project project,
    Settings settings, FileIndexer indexer,
    ModuleFileSystemInitializer initializer,
    ComponentIndexer componentIndexer) {
    this(moduleInputFileCache, project.getKey(), settings, indexer, initializer, componentIndexer);
  }

  private DefaultModuleFileSystem(ModuleInputFileCache moduleInputFileCache, String moduleKey, Settings settings,
    FileIndexer indexer, ModuleFileSystemInitializer initializer,
    @Nullable ComponentIndexer componentIndexer) {
    super(moduleInputFileCache);
    this.componentIndexer = componentIndexer;
    this.moduleKey = moduleKey;
    this.settings = settings;
    this.indexer = indexer;
    if (initializer.baseDir() != null) {
      setBaseDir(initializer.baseDir());
    }
    setWorkDir(initializer.workingDir());
    this.buildDir = initializer.buildDir();
    this.sourceDirsOrFiles = initializer.sources();
    this.testDirsOrFiles = initializer.tests();
    this.binaryDirs = initializer.binaryDirs();
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
    return keepOnlyDirs(sourceDirsOrFiles);
  }

  public List<File> sources() {
    return sourceDirsOrFiles;
  }

  @Override
  public List<File> testDirs() {
    return keepOnlyDirs(testDirsOrFiles);
  }

  public List<File> tests() {
    return testDirsOrFiles;
  }

  private List<File> keepOnlyDirs(List<File> dirsOrFiles) {
    List<File> result = new ArrayList<File>();
    for (File f : dirsOrFiles) {
      if (f.isDirectory()) {
        result.add(f);
      }
    }
    return result;
  }

  @Override
  public List<File> binaryDirs() {
    return binaryDirs;
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
   *
   * @deprecated since 4.0
   */
  @Deprecated
  void addSourceDir(File dir) {
    throw modificationNotPermitted();
  }

  /**
   * Should not be used - only for old plugins
   *
   * @deprecated since 4.0
   */
  @Deprecated
  void addTestDir(File dir) {
    throw modificationNotPermitted();
  }

  private UnsupportedOperationException modificationNotPermitted() {
    return new UnsupportedOperationException("Modifications of the file system are not permitted");
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
    return ImmutableList.copyOf(files(predicates().and(predicates)));
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
      throw MessageException.of("Module filesystem is already initialized. Modifications of filesystem are only allowed during Initializer phase.");
    }
    setBaseDir(basedir);
    this.buildDir = buildDir;
    this.sourceDirsOrFiles = existingDirsOrFiles(sourceDirs);
    this.testDirsOrFiles = existingDirsOrFiles(testDirs);
    this.binaryDirs = existingDirsOrFiles(binaryDirs);
  }

  public void index() {
    if (initialized) {
      throw MessageException.of("Module filesystem can only be indexed once");
    }
    initialized = true;
    indexer.index(this);
    if (componentIndexer != null) {
      componentIndexer.execute(this);
    }
  }

  private List<File> existingDirsOrFiles(List<File> dirsOrFiles) {
    ImmutableList.Builder<File> builder = ImmutableList.builder();
    for (File dirOrFile : dirsOrFiles) {
      if (dirOrFile.exists()) {
        builder.add(dirOrFile);
      }
    }
    return builder.build();
  }

  private FilePredicate fromDeprecatedAttribute(String key, Collection<String> value) {
    if ("TYPE".equals(key)) {
      return predicates().or(Collections2.transform(value, new Function<String, FilePredicate>() {
        @Override
        public FilePredicate apply(@Nullable String s) {
          return s == null ? predicates().all() : predicates().hasType(org.sonar.api.batch.fs.InputFile.Type.valueOf(s));
        }
      }));
    }
    if ("STATUS".equals(key)) {
      return predicates().or(Collections2.transform(value, new Function<String, FilePredicate>() {
        @Override
        public FilePredicate apply(@Nullable String s) {
          return s == null ? predicates().all() : predicates().hasStatus(org.sonar.api.batch.fs.InputFile.Status.valueOf(s));
        }
      }));
    }
    if ("LANG".equals(key)) {
      return predicates().or(Collections2.transform(value, new Function<String, FilePredicate>() {
        @Override
        public FilePredicate apply(@Nullable String s) {
          return s == null ? predicates().all() : predicates().hasLanguage(s);
        }
      }));
    }
    if ("CMP_KEY".equals(key)) {
      return predicates().or(Collections2.transform(value, new Function<String, FilePredicate>() {
        @Override
        public FilePredicate apply(@Nullable String s) {
          return s == null ? predicates().all() : new AdditionalFilePredicates.KeyPredicate(s);
        }
      }));
    }
    if ("CMP_DEPRECATED_KEY".equals(key)) {
      return predicates().or(Collections2.transform(value, new Function<String, FilePredicate>() {
        @Override
        public FilePredicate apply(@Nullable String s) {
          return s == null ? predicates().all() : new AdditionalFilePredicates.DeprecatedKeyPredicate(s);
        }
      }));
    }
    if ("SRC_REL_PATH".equals(key)) {
      return predicates().or(Collections2.transform(value, new Function<String, FilePredicate>() {
        @Override
        public FilePredicate apply(@Nullable String s) {
          return s == null ? predicates().all() : new AdditionalFilePredicates.SourceRelativePathPredicate(s);
        }
      }));
    }
    if ("SRC_DIR_PATH".equals(key)) {
      return predicates().or(Collections2.transform(value, new Function<String, FilePredicate>() {
        @Override
        public FilePredicate apply(@Nullable String s) {
          return s == null ? predicates().all() : new AdditionalFilePredicates.SourceDirPredicate(s);
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
