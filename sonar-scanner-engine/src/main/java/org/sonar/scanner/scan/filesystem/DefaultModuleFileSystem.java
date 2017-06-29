/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.scan.filesystem;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.analysis.DefaultAnalysisMode;
import org.sonar.scanner.repository.ProjectRepositories;

/**
 * @since 3.5
 */
public class DefaultModuleFileSystem extends DefaultFileSystem {

  private String moduleKey;
  private FileIndexer indexer;
  private Configuration settings;

  private List<File> sourceDirsOrFiles = new ArrayList<>();
  private List<File> testDirsOrFiles = new ArrayList<>();
  private boolean initialized;
  private Charset charset = null;

  public DefaultModuleFileSystem(ModuleInputComponentStore moduleInputFileCache, DefaultInputModule module,
    Configuration settings, FileIndexer indexer, ModuleFileSystemInitializer initializer, DefaultAnalysisMode mode,
    ProjectRepositories projectRepositories) {
    super(initializer.baseDir(), moduleInputFileCache);
    setFields(module, settings, indexer, initializer, mode, projectRepositories);
  }

  @VisibleForTesting
  public DefaultModuleFileSystem(DefaultInputModule module,
    Configuration settings, FileIndexer indexer, ModuleFileSystemInitializer initializer, DefaultAnalysisMode mode,
    ProjectRepositories projectRepositories) {
    super(initializer.baseDir().toPath());
    setFields(module, settings, indexer, initializer, mode, projectRepositories);
  }

  private void setFields(DefaultInputModule module,
    Configuration settings, FileIndexer indexer, ModuleFileSystemInitializer initializer, DefaultAnalysisMode mode,
    ProjectRepositories projectRepositories) {
    this.moduleKey = module.key();
    this.settings = settings;
    this.indexer = indexer;
    setWorkDir(initializer.workingDir());
    this.sourceDirsOrFiles = initializer.sources();
    this.testDirsOrFiles = initializer.tests();

    // filter the files sensors have access to
    if (!mode.scanAllFiles()) {
      setDefaultPredicate(new SameInputFilePredicate(projectRepositories, module.definition().getKeyWithBranch()));
    }
  }

  public boolean isInitialized() {
    return initialized;
  }

  public String moduleKey() {
    return moduleKey;
  }

  public List<File> sources() {
    return sourceDirsOrFiles;
  }

  public List<File> tests() {
    return testDirsOrFiles;
  }

  @Override
  public Charset encoding() {
    if (charset == null) {
      String encoding = settings.get(CoreProperties.ENCODING_PROPERTY).orElse(null);
      if (StringUtils.isNotEmpty(encoding)) {
        charset = Charset.forName(StringUtils.trim(encoding));
      } else {
        charset = Charset.defaultCharset();
      }
    }
    return charset;
  }

  @Override
  public boolean isDefaultJvmEncoding() {
    return !settings.hasKey(CoreProperties.ENCODING_PROPERTY);
  }

  @Override
  protected void doPreloadFiles() {
    if (!initialized) {
      throw MessageException.of("Accessing the filesystem before the Sensor phase is not supported. Please update your plugin.");
    }
  }

  public void index() {
    if (initialized) {
      throw MessageException.of("Module filesystem can only be indexed once");
    }
    initialized = true;
    indexer.index(this);
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
