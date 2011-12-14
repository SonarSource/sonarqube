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
package org.sonar.java.ast.check;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.visitor.AstUtils;
import org.sonar.java.ast.visitor.JavaAstVisitor;
import org.sonar.java.recognizer.JavaFootprint;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.recognizer.CodeRecognizer;

import com.google.common.collect.Sets;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TextBlock;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * @since 2.13
 */
@Rule(key = "CommentedOutCodeLine", priority = Priority.MAJOR)
public class CommentedOutCodeLineCheck extends JavaAstVisitor {

  private static final double THRESHOLD = 0.9;

  /**
   * This list was taken from com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocStyleCheck
   */
  private static final List<Integer> WANTED_TOKENS = Arrays.asList(
      TokenTypes.INTERFACE_DEF,
      TokenTypes.CLASS_DEF,
      TokenTypes.ANNOTATION_DEF,
      TokenTypes.ENUM_DEF,
      TokenTypes.METHOD_DEF,
      TokenTypes.CTOR_DEF,
      TokenTypes.VARIABLE_DEF,
      TokenTypes.ENUM_CONSTANT_DEF,
      TokenTypes.ANNOTATION_FIELD_DEF,
      TokenTypes.PACKAGE_DEF);

  private final CodeRecognizer codeRecognizer;
  private Set<TextBlock> comments;

  public CommentedOutCodeLineCheck() {
    codeRecognizer = new CodeRecognizer(THRESHOLD, new JavaFootprint());
  }

  @Override
  public List<Integer> getWantedTokens() {
    return WANTED_TOKENS;
  }

  /**
   * Creates candidates for commented-out code - all comment blocks.
   */
  @Override
  public void visitFile(DetailAST ast) {
    comments = Sets.newHashSet();
    for (TextBlock comment : getFileContents().getCppComments().values()) {
      comments.add(comment);
    }
    for (List<TextBlock> listOfComments : getFileContents().getCComments().values()) {
      // This list contains not only comments in C style, but also documentation comments and JSNI comments
      comments.addAll(listOfComments);
    }
  }

  /**
   * Removes documentation comments and JSNI comments from candidates for commented-out code in order to prevent false-positives.
   */
  @Override
  public void visitToken(DetailAST ast) {
    if (canBeDocumented(ast)) {
      TextBlock javadoc = getFileContents().getJavadocBefore(ast.getLineNo());
      if (javadoc != null) {
        comments.remove(javadoc);
      }
    }
    removeJSNIComments(ast);
  }

  /**
   * From documentation for Javadoc-tool:
   * Documentation comments should be recognized only when placed
   * immediately before class, interface, constructor, method, or field declarations.
   */
  private static boolean canBeDocumented(DetailAST ast) {
    if (AstUtils.isType(ast, TokenTypes.VARIABLE_DEF)) {
      return AstUtils.isClassVariable(ast);
    }
    return true;
  }

  /**
   * Detects commented-out code in remaining candidates.
   */
  @Override
  public void leaveFile(DetailAST ast) {
    SourceFile sourceFile = (SourceFile) peekSourceCode();
    for (TextBlock comment : comments) {
      String[] lines = comment.getText();
      for (int i = 0; i < lines.length; i++) {
        if (codeRecognizer.isLineOfCode(lines[i])) {
          CheckMessage message = new CheckMessage(this, "It's better to remove commented-out line of code.");
          message.setLine(comment.getStartLineNo() + i);
          sourceFile.log(message);
          break;
        }
      }
    }
    comments = null;
  }

  /**
   * From GWT documentation:
   * JSNI methods are declared native and contain JavaScript code in a specially formatted comment block
   * between the end of the parameter list and the trailing semicolon.
   * A JSNI comment block begins with the exact token {@link #START_JSNI} and ends with the exact token {@link #END_JSNI}.
   */
  private void removeJSNIComments(DetailAST ast) {
    if (AstUtils.isType(ast, TokenTypes.METHOD_DEF) && AstUtils.isModifier(ast, TokenTypes.LITERAL_NATIVE)) {
      DetailAST endOfParameterList = ast.findFirstToken(TokenTypes.PARAMETERS).getNextSibling();
      DetailAST trailingSemicolon = ast.getLastChild();

      for (int lineNumber = endOfParameterList.getLineNo(); lineNumber <= trailingSemicolon.getLineNo(); lineNumber++) {
        List<TextBlock> listOfComments = getFileContents().getCComments().get(lineNumber);
        if (listOfComments != null) {
          for (TextBlock comment : listOfComments) {
            if (isJSNI(comment) && isCommentBetween(comment, endOfParameterList, trailingSemicolon)) {
              comments.remove(comment);
            }
          }
        }
      }
    }
  }

  private static boolean isCommentBetween(TextBlock comment, DetailAST start, DetailAST end) {
    return comment.intersects(start.getLineNo(), start.getColumnNo(), end.getLineNo(), end.getColumnNo());
  }

  private static final String START_JSNI = "/*-{";
  private static final String END_JSNI = "}-*/";

  private boolean isJSNI(TextBlock comment) {
    String[] lines = comment.getText();
    String firstLine = lines[0];
    String lastLine = lines[lines.length - 1];
    return StringUtils.startsWith(firstLine, START_JSNI) && StringUtils.endsWith(lastLine, END_JSNI);
  }

}
