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

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.InputFile;
import org.sonar.java.ast.visitor.JavaAstVisitor;
import org.sonar.squid.text.Source;

/**
 * Delegate from Checkstyle {@link Check} to {@link JavaAstVisitor}s.
 */
public class CheckstyleSquidBridge extends Check {

  private static Logger logger = LoggerFactory.getLogger(CheckstyleSquidBridge.class);

  private static CheckstyleSquidBridgeContext bridgeContext;

  /**
   * @see CheckstyleSquidBridgeContext
   */
  static void setContext(CheckstyleSquidBridgeContext context) {
    bridgeContext = context;
  }

  @Override
  public int[] getDefaultTokens() {
    return bridgeContext.getAllTokens();
  }

  @Override
  public void beginTree(DetailAST ast) {
    try {
      String filename = getFileContents().getFilename();
      Source source = createSource();
      InputFile inputFile = bridgeContext.getInputFile(new java.io.File(filename));
      for (JavaAstVisitor visitor : bridgeContext.getVisitors()) {
        visitor.setFileContents(getFileContents());
        visitor.setSource(source);
        visitor.setInputFile(inputFile);
        visitor.visitFile(ast);
      }
    } catch (RuntimeException e) {
      logAndThrowException(e);
    }
  }

  private Source createSource() {
    return new Source(getFileContents().getLines(), bridgeContext.getCodeRecognizer());
  }

  @Override
  public void visitToken(DetailAST ast) {
    try {
      for (JavaAstVisitor visitor : bridgeContext.getVisitors()) {
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
    JavaAstVisitor[] visitors = bridgeContext.getVisitors();
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
    JavaAstVisitor[] visitors = bridgeContext.getVisitors();
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
