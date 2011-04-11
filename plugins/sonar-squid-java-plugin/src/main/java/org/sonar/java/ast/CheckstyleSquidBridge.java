/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import java.io.File;
import java.util.*;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.InputFile;
import org.sonar.java.ast.visitor.JavaAstVisitor;
import org.sonar.java.recognizer.JavaFootprint;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.recognizer.CodeRecognizer;
import org.sonar.squid.text.Source;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;

public class CheckstyleSquidBridge extends Check {

  private static Logger logger = LoggerFactory.getLogger(CheckstyleSquidBridge.class);
  private static JavaAstVisitor[] visitors;
  private static int[] allTokens;
  private static CodeRecognizer codeRecognizer;
  private static Map<java.io.File,InputFile> inputFilesByPath = Maps.newHashMap();

  static void setASTVisitors(List<JavaAstVisitor> visitors) {
    CheckstyleSquidBridge.visitors = visitors.toArray(new JavaAstVisitor[visitors.size()]);
    SortedSet<Integer> sorter = new TreeSet<Integer>();
    for (JavaAstVisitor visitor : visitors) {
      sorter.addAll(visitor.getWantedTokens());
      allTokens = new int[sorter.size()];
      int i = 0;
      for (Integer itSorted : sorter) {
        allTokens[i++] = itSorted;
      }
    }
  }

  static void setSquidConfiguration(JavaSquidConfiguration conf) {
    codeRecognizer = new CodeRecognizer(conf.getCommentedCodeThreshold(), new JavaFootprint());
  }

  @Override
  public int[] getDefaultTokens() {
    return allTokens; //NOSONAR returning directly the array is not a security flaw here
  }

  public static InputFile getInputFile(File path) {
    return inputFilesByPath.get(path);
  }

  public static void setInputFiles(Collection<InputFile> inputFiles) {
    inputFilesByPath.clear();
    for (InputFile inputFile : inputFiles) {
      inputFilesByPath.put(inputFile.getFile(), inputFile);
    }
  }

  @Override
  public void beginTree(DetailAST ast) {
    try {
      Source source = createSource();
      for (JavaAstVisitor visitor : visitors) {
        visitor.setFileContents(getFileContents());
        visitor.setSource(source);
        visitor.setInputFile(getInputFile(new java.io.File(getFileContents().getFilename())));
        visitor.visitFile(ast);
      }
    } catch (RuntimeException e) {
      logAndThrowException(e);
    }
  }

  private Source createSource() {
    return new Source(getFileContents().getLines(), codeRecognizer);
  }

  @Override
  public void visitToken(DetailAST ast) {
    try {
      for (JavaAstVisitor visitor : visitors) {
        if (visitor.getWantedTokens().contains(ast.getType())) {
          visitor.visitToken(ast);
        }
      }
    } catch (RuntimeException e) {
      logAndThrowException(e);
    }
  }

  @Override
  public void leaveToken(DetailAST ast) {
    try {
      for (int i = visitors.length - 1; i >= 0; i--) {
        JavaAstVisitor visitor = visitors[i];
        if (visitor.getWantedTokens().contains(ast.getType())) {
          visitor.leaveToken(ast);
        }
      }
    } catch (RuntimeException e) {
      logAndThrowException(e);
    }
  }

  @Override
  public void finishTree(DetailAST ast) {
    try {
      for (int i = visitors.length - 1; i >= 0; i--) {
        JavaAstVisitor visitor = visitors[i];
        visitor.leaveFile(ast);
      }
    } catch (RuntimeException e) {
      logAndThrowException(e);
    }
  }

  private void logAndThrowException(RuntimeException e) {
    logger.error("Squid Error occurs when analysing :" + getFileContents().getFilename(), e);
    throw e;
  }

}
