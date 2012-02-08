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

import com.google.common.collect.Maps;
import org.sonar.api.resources.InputFile;
import org.sonar.java.ast.visitor.JavaAstVisitor;
import org.sonar.java.recognizer.JavaFootprint;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.recognizer.CodeRecognizer;

import java.io.File;
import java.util.*;

/**
 * This class helps to transfer additional information into {@link CheckstyleSquidBridge}.
 * We forced to use static fields in {@link CheckstyleSquidBridge},
 * because it will be instantiated by Checkstyle, so there is no other way of communication.
 */
class CheckstyleSquidBridgeContext {

  private int[] allTokens;
  private JavaAstVisitor[] visitors;
  private CodeRecognizer codeRecognizer;
  private Map<java.io.File, InputFile> inputFilesByPath = Maps.newHashMap();

  public CheckstyleSquidBridgeContext setASTVisitors(List<JavaAstVisitor> visitors) {
    this.visitors = visitors.toArray(new JavaAstVisitor[visitors.size()]);
    SortedSet<Integer> sorter = new TreeSet<Integer>();
    for (JavaAstVisitor visitor : visitors) {
      sorter.addAll(visitor.getWantedTokens());
      allTokens = new int[sorter.size()];
      int i = 0;
      for (Integer itSorted : sorter) {
        allTokens[i++] = itSorted;
      }
    }
    return this;
  }

  public CheckstyleSquidBridgeContext setSquidConfiguration(JavaSquidConfiguration conf) {
    codeRecognizer = new CodeRecognizer(conf.getCommentedCodeThreshold(), new JavaFootprint());
    return this;
  }

  public CheckstyleSquidBridgeContext setInputFiles(Collection<InputFile> inputFiles) {
    inputFilesByPath.clear();
    for (InputFile inputFile : inputFiles) {
      inputFilesByPath.put(inputFile.getFile().getAbsoluteFile(), inputFile);
    }
    return this;
  }

  public int[] getAllTokens() {
    return allTokens;
  }

  public InputFile getInputFile(File path) {
    return inputFilesByPath.get(path);
  }

  public CodeRecognizer getCodeRecognizer() {
    return codeRecognizer;
  }

  public JavaAstVisitor[] getVisitors() {
    return visitors;
  }

}
