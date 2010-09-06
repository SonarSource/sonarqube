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

import java.util.ArrayList;
import java.util.List;

public class ParameterSignatureScanner {

  private final String signature;
  private int index = 0;

  private static final char ARRAY = '[';

  private ParameterSignatureScanner(String parametersSignature) {
    this.signature = parametersSignature;
  }

  public static Parameter scan(String parameterSignature) {
    ParameterSignatureScanner scanner = new ParameterSignatureScanner(parameterSignature);
    if (scanner.hasNext()) {
      return scanner.next();
    } else {
      return null;
    }
  }

  public static List<Parameter> scanArguments(String argumentsSignature) {
    List<Parameter> arguments = new ArrayList<Parameter>();

    ParameterSignatureScanner scanner = new ParameterSignatureScanner(argumentsSignature);
    while (scanner.hasNext()) {
      arguments.add(scanner.next());
    }

    return arguments;
  }

  private boolean hasNext() {
    if (signature.length() > index && (signature.charAt(index) == ARRAY || nextCharIsJvmJavaType())) {
      return true;
    }
    return false;
  }

  private boolean nextCharIsJvmJavaType() {
    try {
      JvmJavaType.valueOf(signature.substring(index, index + 1));
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private Parameter next() {
    boolean isArray = false;
    String classCanonicalName = null;

    while (signature.charAt(index) == ARRAY) {
      isArray = true;
      index++;
    }

    JvmJavaType jvmJavaType = JvmJavaType.valueOf(signature.substring(index, index + 1));
    index = index + 1;

    if (jvmJavaType == JvmJavaType.L || jvmJavaType == JvmJavaType.T) {
      int semicolonIndex = searchEndOfParameterSignature(signature, index);
      if (signature.indexOf('<', index) != -1) {
        classCanonicalName = signature.substring(index, signature.indexOf('<', index));
      } else {
        classCanonicalName = signature.substring(index, semicolonIndex);
      }
      index = semicolonIndex + 1;
      jvmJavaType = JvmJavaType.L;
    }
    return new Parameter(jvmJavaType, classCanonicalName, isArray);
  }

  private int searchEndOfParameterSignature(String signature, int index) {
    int genericDefinitionStack = 0;
    for (; index < signature.length(); index++) {
      char character = signature.charAt(index);
      if (character == ';' && genericDefinitionStack == 0) {
        return index;
      }
      if (character == '<') {
        genericDefinitionStack++;
      } else if (character == '>') {
        genericDefinitionStack--;
      }
    }
    throw new IllegalStateException("Unable to extract parameter signature from '" + signature + "'");
  }
}
