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
package org.sonar.java.signature;

import java.util.List;

public class MethodSignatureScanner {

  private final String bytecodeMethodSignature;

  private MethodSignatureScanner(String bytecodeMethodSignature) {
    this.bytecodeMethodSignature = bytecodeMethodSignature;
  }

  public static MethodSignature scan(String bytecodeMethodSignature) {
    MethodSignatureScanner scanner = new MethodSignatureScanner(bytecodeMethodSignature);
    return scanner.scan();
  }

  private MethodSignature scan() {
    int leftBracketIndex = bytecodeMethodSignature.indexOf('(');
    int rightBracketIndex = bytecodeMethodSignature.indexOf(')');
    String methodName = bytecodeMethodSignature.substring(0, leftBracketIndex);
    Parameter returnType = ParameterSignatureScanner.scan(bytecodeMethodSignature.substring(rightBracketIndex + 1));
    List<Parameter> argumentTypes = ParameterSignatureScanner.scanArguments(bytecodeMethodSignature.substring(leftBracketIndex + 1,
        rightBracketIndex));
    return new MethodSignature(methodName, returnType, argumentTypes);
  }
}
