/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.squid;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.CheckFactory;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.bytecode.BytecodeScanner;
import org.sonar.java.bytecode.check.BytecodeCheck;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.plugins.squid.bridges.Bridge;
import org.sonar.plugins.squid.bridges.BridgeFactory;
import org.sonar.plugins.squid.bridges.ResourceIndex;
import org.sonar.squid.Squid;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourcePackage;
import org.sonar.squid.indexer.QueryByType;
import org.sonar.squid.measures.Metric;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

public final class SquidExecutor {

  private Squid squid;
  private boolean sourceScanned = false;
  private boolean bytecodeScanned = false;
  private CheckFactory checkFactory;

  public SquidExecutor(boolean analyzePropertyAccessors, String fieldNamesToExcludeFromLcom4Computation, CheckFactory checkFactory, Charset sourcesCharset) {
    JavaSquidConfiguration conf = createJavaSquidConfiguration(analyzePropertyAccessors, fieldNamesToExcludeFromLcom4Computation,
        sourcesCharset);
    squid = new Squid(conf);
    this.checkFactory = checkFactory;
  }

  private JavaSquidConfiguration createJavaSquidConfiguration(boolean analyzePropertyAccessors,
      String fieldNamesToExcludeFromLcom4Computation,
      Charset sourcesCharset) {
    JavaSquidConfiguration conf = new JavaSquidConfiguration(analyzePropertyAccessors, sourcesCharset);

    if ( !StringUtils.isBlank(fieldNamesToExcludeFromLcom4Computation)) {
      for (String fieldName : fieldNamesToExcludeFromLcom4Computation.split(",")) {
        if ( !StringUtils.isBlank(fieldName)) {
          conf.addFieldToExcludeFromLcom4Calculation(fieldName);
        }
      }
    }
    return conf;
  }

  protected SquidExecutor(Squid squid) {
    this.squid = squid;
  }

  public void scan(Collection<File> sourceFiles, Collection<File> bytecodeFilesOrDirectories) {
    scanSources(sourceFiles);
    if (sourceScanned) {
      for (Object checker : checkFactory.getChecks()) {
        squid.registerVisitor((BytecodeCheck) checker);
      }
      scanBytecode(bytecodeFilesOrDirectories);
    }
    squid.decorateSourceCodeTreeWith(Metric.values());
  }

  public void save(Project project, SensorContext context, NoSonarFilter noSonarFilter) {
    if (sourceScanned) {
      TimeProfiler profiler = new TimeProfiler(getClass()).start("Squid extraction");
      ResourceIndex resourceIndex = new ResourceIndex().loadSquidResources(squid, context, project);
      List<Bridge> bridges = BridgeFactory.create(bytecodeScanned, context, checkFactory, resourceIndex, squid, noSonarFilter);
      saveProject(resourceIndex, bridges);
      savePackages(resourceIndex, bridges);
      saveFiles(resourceIndex, bridges);
      profiler.stop();
    }
  }

  private void saveProject(ResourceIndex resourceIndex, List<Bridge> bridges) {
    Resource sonarResource = resourceIndex.get(squid.getProject());
    for (Bridge bridge : bridges) {
      bridge.onProject(squid.getProject(), (Project) sonarResource);
    }
  }

  private void savePackages(ResourceIndex resourceIndex, List<Bridge> bridges) {
    Collection<SourceCode> packages = squid.search(new QueryByType(SourcePackage.class));
    for (SourceCode squidPackage : packages) {
      Resource sonarPackage = resourceIndex.get(squidPackage);
      for (Bridge bridge : bridges) {
        bridge.onPackage((SourcePackage) squidPackage, sonarPackage);
      }
    }
  }

  private void saveFiles(ResourceIndex resourceIndex, List<Bridge> bridges) {
    Collection<SourceCode> squidFiles = squid.search(new QueryByType(SourceFile.class));
    for (SourceCode squidFile : squidFiles) {
      Resource sonarFile = resourceIndex.get(squidFile);
      for (Bridge bridge : bridges) {
        bridge.onFile((SourceFile) squidFile, sonarFile);
      }
    }
  }

  void scanSources(Collection<File> sourceFiles) {
    if (sourceFiles != null && !sourceFiles.isEmpty()) {
      TimeProfiler profiler = new TimeProfiler(getClass()).start("Java AST scan");
      JavaAstScanner sourceScanner = squid.register(JavaAstScanner.class);
      sourceScanner.scanFiles(sourceFiles);
      sourceScanned = true;
      profiler.stop();

    } else {
      sourceScanned = false;
    }
  }

  void scanBytecode(Collection<File> bytecodeFilesOrDirectories) {
    if (bytecodeFilesOrDirectories != null && !bytecodeFilesOrDirectories.isEmpty()) {
      TimeProfiler profiler = new TimeProfiler(getClass()).start("Java bytecode scan");
      BytecodeScanner bytecodeScanner = squid.register(BytecodeScanner.class);
      bytecodeScanner.scan(bytecodeFilesOrDirectories);
      bytecodeScanned = true;
      profiler.stop();
    } else {
      bytecodeScanned = false;
    }
  }

  boolean isSourceScanned() {
    return sourceScanned;
  }

  boolean isBytecodeScanned() {
    return bytecodeScanned;
  }

  public void initSonarProxy(SquidSearchProxy proxy) {
    squid.flush();
    proxy.setTarget(squid);
  }

  public Squid getSquid() {
    return squid;
  }
}
