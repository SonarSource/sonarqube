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
package org.sonar.java.bytecode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmClassProvider;
import org.sonar.java.bytecode.asm.AsmClassProviderImpl;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.bytecode.asm.AsmClassProvider.DETAIL_LEVEL;
import org.sonar.java.bytecode.visitor.AccessorVisitor;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.java.bytecode.visitor.DITVisitor;
import org.sonar.java.bytecode.visitor.DependenciesVisitor;
import org.sonar.java.bytecode.visitor.LCOM4Visitor;
import org.sonar.java.bytecode.visitor.NOCVisitor;
import org.sonar.java.bytecode.visitor.RFCVisitor;
import org.sonar.squid.api.CodeScanner;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.indexer.QueryByType;
import org.sonar.squid.indexer.SquidIndex;

public class BytecodeScanner extends CodeScanner<BytecodeVisitor> {

  private SquidIndex indexer;

  public BytecodeScanner(SquidIndex indexer) {
    this.indexer = indexer;
  }

  public BytecodeScanner scan(Collection<File> bytecodeFilesOrDirectories) {
    Collection<SourceCode> classes = indexer.search(new QueryByType(SourceClass.class));
    return scan(classes, new AsmClassProviderImpl(ClassworldsClassLoader.create(bytecodeFilesOrDirectories)));
  }

  public BytecodeScanner scanDirectory(File bytecodeDirectory) {
    return scan(Arrays.asList(bytecodeDirectory));
  }

  protected BytecodeScanner scan(Collection<SourceCode> classes, AsmClassProvider classProvider) {
    loadByteCodeInformation(classes, classProvider);
    linkVirtualMethods(classes, classProvider);
    notifyBytecodeVisitors(classes, classProvider);
    return this;
  }

  private void linkVirtualMethods(Collection<SourceCode> classes, AsmClassProvider classProvider) {
    VirtualMethodsLinker linker = new VirtualMethodsLinker();
    for (SourceCode sourceCode : classes) {
      AsmClass asmClass = classProvider.getClass(sourceCode.getKey(), DETAIL_LEVEL.STRUCTURE_AND_CALLS);
      for (AsmMethod method : asmClass.getMethods()) {
        linker.process(method);
      }
    }
  }

  private void notifyBytecodeVisitors(Collection<SourceCode> classes, AsmClassProvider classProvider) {
    BytecodeVisitor[] visitorArray = getVisitors().toArray(new BytecodeVisitor[getVisitors().size()]);
    for (SourceCode sourceCode : classes) {
      AsmClass asmClass = classProvider.getClass(sourceCode.getKey(), DETAIL_LEVEL.STRUCTURE_AND_CALLS);
      BytecodeVisitorNotifier visitorNotifier = new BytecodeVisitorNotifier(asmClass, visitorArray);
      visitorNotifier.notifyVisitors(indexer);
    }
  }

  private void loadByteCodeInformation(Collection<SourceCode> classes, AsmClassProvider classProvider) {
    for (SourceCode sourceCode : classes) {
      classProvider.getClass(sourceCode.getKey(), DETAIL_LEVEL.STRUCTURE_AND_CALLS);
    }
  }

  @Override
  public Collection<Class<? extends BytecodeVisitor>> getVisitorClasses() {
    List<Class<? extends BytecodeVisitor>> visitorClasses = new ArrayList<Class<? extends BytecodeVisitor>>();
    visitorClasses.add(AccessorVisitor.class);
    visitorClasses.add(DITVisitor.class);
    visitorClasses.add(RFCVisitor.class);
    visitorClasses.add(NOCVisitor.class);
    visitorClasses.add(LCOM4Visitor.class);
    visitorClasses.add(DependenciesVisitor.class);
    return visitorClasses;
  }
}
