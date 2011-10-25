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
package org.sonar.java.bytecode.visitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmEdge;
import org.sonar.java.bytecode.asm.AsmField;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.bytecode.asm.AsmResource;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.api.SourceCodeEdgeUsage;
import org.sonar.squid.measures.Metric;

public class LCOM4Visitor extends BytecodeVisitor {

  private AsmClass asmClass;
  private List<Set<AsmResource>> unrelatedBlocks = null;
  private final Set<String> fieldsToExcludeFromLcom4Calculation;

  public LCOM4Visitor(JavaSquidConfiguration conf) {
    this.fieldsToExcludeFromLcom4Calculation = conf.getFielsToExcludeFromLcom4Calculation();
  }

  public void visitClass(AsmClass asmClass) {
    this.asmClass = asmClass;
    unrelatedBlocks = new ArrayList<Set<AsmResource>>();
  }

  public void processMethod(AsmMethod asmMethod) {
    if (isMethodElligibleForLCOM4Computation(asmMethod)) {
      ensureBlockIsCreated(asmMethod);
      for (AsmEdge edge : asmMethod.getOutgoingEdges()) {
        if (isCallToInternalFieldOrMethod(edge) && isNotCallToExcludedFieldFromLcom4Calculation(edge.getTo())) {
          AsmResource toResource = getAccessedFieldOrMethod(edge.getTo());
          linkAsmResources(asmMethod, toResource);
        }
      }
    }
  }
  
  private AsmResource getAccessedFieldOrMethod(AsmResource resource) {
    if (resource instanceof AsmMethod && ((AsmMethod)resource).isAccessor()) {
      return ((AsmMethod)resource).getAccessedField();
    } else {
      return resource;
    }
  }
  
  private boolean isNotCallToExcludedFieldFromLcom4Calculation(AsmResource to) {
    if (to instanceof AsmField) {
      AsmField field = (AsmField)to;
      return !fieldsToExcludeFromLcom4Calculation.contains(field.getName());
    }
    return true;
  }

  private boolean isMethodElligibleForLCOM4Computation(AsmMethod asmMethod) {
    return !asmMethod.isAbstract() && !asmMethod.isStatic() && !asmMethod.isConstructor() && !asmMethod.isEmpty()
        && !asmMethod.isAccessor() && asmMethod.isBodyLoaded();
  }

  public void leaveClass(AsmClass asmClass) {
    for (AsmMethod asmMethod: asmClass.getMethods()) {
      processMethod(asmMethod);
    }
    
    int lcom4 = unrelatedBlocks.size();
    if (lcom4 == 0) {
      lcom4 = 1;
    }

    getSourceClass(asmClass).add(Metric.LCOM4, lcom4);
    getSourceClass(asmClass).addData(Metric.LCOM4_BLOCKS, unrelatedBlocks);

    if (isMainPublicClassInFile(asmClass)) {
      getSourceFile(asmClass).add(Metric.LCOM4, lcom4);
      getSourceFile(asmClass).addData(Metric.LCOM4_BLOCKS, unrelatedBlocks);
    }
  }
  
  private void ensureBlockIsCreated(AsmResource resource) {
    getOrCreateResourceBlock(resource);
  }
  
  private void linkAsmResources(AsmResource resourceA, AsmResource resourceB) {
    Set<AsmResource> blockA = getOrCreateResourceBlock(resourceA);
    Set<AsmResource> blockB = getOrCreateResourceBlock(resourceB);
    
    // getOrCreateResourceBlock() returns the same block instance if resourceA and resourceB are identical or already in the same block
    if (blockA == blockB) { // NOSONAR false-positive Compare Objects With Equals
      return;
    }
    
    blockA.addAll(blockB);
    unrelatedBlocks.remove(blockB);
  }
  
  private boolean isCallToInternalFieldOrMethod(AsmEdge edge) {
    return edge.getTargetAsmClass() == asmClass && (edge.getUsage() == SourceCodeEdgeUsage.CALLS_FIELD || edge.getTargetAsmClass() == asmClass && edge.getUsage() == SourceCodeEdgeUsage.CALLS_METHOD);
  }

  private Set<AsmResource> getOrCreateResourceBlock(AsmResource resource) {
    for (Set<AsmResource> block: unrelatedBlocks) {
      if (block.contains(resource)) {
        return block;
      }
    }

    Set<AsmResource> block = new HashSet<AsmResource>();
    block.add(resource);
    unrelatedBlocks.add(block);
    return block;
  }
}
