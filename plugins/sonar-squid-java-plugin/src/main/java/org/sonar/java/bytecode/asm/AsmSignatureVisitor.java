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
package org.sonar.java.bytecode.asm;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.signature.SignatureVisitor;

public class AsmSignatureVisitor implements SignatureVisitor {

  private Set<String> internalNames = new HashSet<String>();

  public Set<String> getInternalNames() {
    return internalNames;
  }

  /**
   * {@inheritDoc}
   */
  public void visitClassType(String name) {
    internalNames.add(name);
  }

  /**
   * {@inheritDoc}
   */
  public SignatureVisitor visitArrayType() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public void visitBaseType(char descriptor) {
  }

  /**
   * {@inheritDoc}
   */
  public SignatureVisitor visitClassBound() {
    return this;
  }

  public SignatureVisitor visitExceptionType() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public void visitFormalTypeParameter(String name) {
  }

  /**
   * {@inheritDoc}
   */
  public void visitInnerClassType(String name) {
  }

  /**
   * {@inheritDoc}
   */
  public SignatureVisitor visitInterface() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public SignatureVisitor visitInterfaceBound() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public SignatureVisitor visitParameterType() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public SignatureVisitor visitReturnType() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public SignatureVisitor visitSuperclass() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public void visitTypeArgument() {
  }

  /**
   * {@inheritDoc}
   */
  public SignatureVisitor visitTypeArgument(char wildcard) {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public void visitTypeVariable(String name) {
  }

  /**
   * {@inheritDoc}
   */
  public void visitEnd() {
  }
}
