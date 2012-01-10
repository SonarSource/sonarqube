/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.java.ast;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.java.ast.visitor.*;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.api.AnalysisException;
import org.sonar.squid.api.CodeScanner;
import org.sonar.squid.api.CodeVisitor;
import org.sonar.squid.api.SourceCode;
import org.xml.sax.InputSource;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.Configuration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Squid uses Checkstyle to get an out-of-the-box java parser with AST generation and visitor pattern support.
 */
public class JavaAstScanner extends CodeScanner<JavaAstVisitor> {

  private static final Logger LOG = LoggerFactory.getLogger(JavaAstScanner.class);
  private JavaSquidConfiguration conf;
  private SourceCode project;

  public JavaAstScanner(JavaSquidConfiguration conf, SourceCode project) {
    this.conf = conf;
    this.project = project;
  }

  private Checker createChecker(Charset charset) {
    InputStream checkstyleConfig = null;
    try {
      checkstyleConfig = JavaAstScanner.class.getClassLoader().getResourceAsStream("checkstyle-configuration.xml");
      String readenConfig = IOUtils.toString(checkstyleConfig);
      readenConfig = readenConfig.replace("${charset}", charset.toString());
      checkstyleConfig = new ByteArrayInputStream(readenConfig.getBytes());
      Configuration config = ConfigurationLoader.loadConfiguration(new InputSource(checkstyleConfig), new PropertiesExpander(System
          .getProperties()), false);
      Checker c = new Checker();
      final ClassLoader moduleClassLoader = Checker.class.getClassLoader();
      c.setModuleClassLoader(moduleClassLoader);
      c.configure(config);
      c.addListener(new CheckstyleAuditListener());
      return c;

    } catch (Exception e) { // NOSONAR We want to be sure to catch any unexpected exception
      throw new AnalysisException(
          "Unable to create Checkstyle Checker object with 'checkstyle-configuration.xml' as Checkstyle configuration file name", e);

    } finally {
      IOUtils.closeQuietly(checkstyleConfig);
    }
  }

  public JavaAstScanner scanDirectory(File javaSourceDirectory) {
    List<InputFile> inputFiles = Lists.newArrayList();
    Collection<File> files = FileUtils.listFiles(javaSourceDirectory, FileFilterUtils.fileFileFilter(), FileFilterUtils.directoryFileFilter());
    for (File file : files) {
      inputFiles.add(InputFileUtils.create(javaSourceDirectory, file));
    }
    return scanFiles(inputFiles);
  }

  public JavaAstScanner scanFile(InputFile javaFile) {
    return scanFiles(Arrays.asList(javaFile));
  }

  public JavaAstScanner scanFiles(Collection<InputFile> inputFiles) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("----- Java sources analyzed by Squid:");
      for (InputFile inputFile : inputFiles) {
        LOG.debug(inputFile.toString());
      }
      LOG.debug("-----");
    }

    Stack<SourceCode> resourcesStack = new Stack<SourceCode>();
    resourcesStack.add(project);
    for (JavaAstVisitor visitor : getVisitors()) {
      visitor.setSourceCodeStack(resourcesStack);
    }
    CheckstyleSquidBridge.setASTVisitors(getVisitors());
    CheckstyleSquidBridge.setSquidConfiguration(conf);
    CheckstyleSquidBridge.setInputFiles(inputFiles);
    launchCheckstyle(InputFileUtils.toFiles(inputFiles), conf.getCharset());
    return this;
  }

  private void launchCheckstyle(Collection<File> files, Charset charset) {
    Checker c = createChecker(charset);
    ClassLoader initialClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    try {
      c.setClassloader(getClass().getClassLoader());
      c.setModuleClassLoader(getClass().getClassLoader());
      c.process(Lists.<File>newArrayList(files));
      c.destroy();
    } finally {
      Thread.currentThread().setContextClassLoader(initialClassLoader);
    }
  }


  @Override
  public Collection<Class<? extends JavaAstVisitor>> getVisitorClasses() {
    List<Class<? extends JavaAstVisitor>> visitorClasses = Lists.newArrayList();
    visitorClasses.add(PackageVisitor.class);
    visitorClasses.add(FileVisitor.class);
    visitorClasses.add(ClassVisitor.class);
    visitorClasses.add(AnonymousInnerClassVisitor.class);
    visitorClasses.add(MethodVisitor.class);
    visitorClasses.add(EndAtLineVisitor.class);
    visitorClasses.add(LinesVisitor.class);
    visitorClasses.add(BlankLinesVisitor.class);
    visitorClasses.add(CommentVisitor.class);
    visitorClasses.add(PublicApiVisitor.class);
    visitorClasses.add(BranchVisitor.class);
    visitorClasses.add(StatementVisitor.class);
    if (conf.isAnalysePropertyAccessors()) {
      visitorClasses.add(AccessorVisitor.class);
    }
    visitorClasses.add(ComplexityVisitor.class);
    visitorClasses.add(LinesOfCodeVisitor.class);
    return visitorClasses;
  }

  @Override
  public void accept(CodeVisitor visitor) {
    if (visitor instanceof JavaAstVisitor) {
      super.accept(visitor);
    }
  }
}
