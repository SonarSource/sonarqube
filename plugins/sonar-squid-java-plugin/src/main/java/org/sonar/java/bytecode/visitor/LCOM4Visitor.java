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

  public void visitMethod(AsmMethod asmMethod) {
    if (isMethodElligibleForLCOM4Computation(asmMethod)) {
      Set<AsmResource> block = getResourceBlockOrCreateIt(asmMethod);
      for (AsmEdge edge : asmMethod.getOutgoingEdges()) {
        if (isCallToInternalFieldOrMethod(edge) && isNotCallToExcludedFieldFromLcom4Calculation(edge.getTo())) {
          AsmResource toResource = edge.getTo();
          mergeAsmResourceToBlock(block, toResource);
        }
      }
    }
  }

  private boolean isNotCallToExcludedFieldFromLcom4Calculation(AsmResource to) {
    if (to instanceof AsmField) {
      AsmField field = (AsmField) to;
      return !fieldsToExcludeFromLcom4Calculation.contains(field.getName());
    }
    return true;
  }

  private boolean isMethodElligibleForLCOM4Computation(AsmMethod asmMethod) {
    return !asmMethod.isAbstract() && !asmMethod.isStatic() && !asmMethod.isConstructor() && !asmMethod.isEmpty()
        && !asmMethod.isAccessor() && asmMethod.isBodyLoaded();
  }

  public void leaveClass(AsmClass asmClass) {
    // filterIsolatedMethods();
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

  private void mergeAsmResourceToBlock(Set<AsmResource> block, AsmResource toResource) {
    if (block.contains(toResource)) {
      return;
    }
    Set<AsmResource> otherBlock = getResourceBlock(toResource);
    if (otherBlock == null) {
      block.add(toResource);

    } else {
      block.addAll(otherBlock);
      unrelatedBlocks.remove(otherBlock);
    }
  }

  private boolean isCallToInternalFieldOrMethod(AsmEdge edge) {
    return edge.getTargetAsmClass() == asmClass
        && (edge.getUsage() == SourceCodeEdgeUsage.CALLS_FIELD || edge.getUsage() == SourceCodeEdgeUsage.CALLS_METHOD);
  }

  private Set<AsmResource> getResourceBlockOrCreateIt(AsmResource fromResource) {
    Set<AsmResource> block = getResourceBlock(fromResource);
    if (block != null) {
      return block;
    }
    block = new HashSet<AsmResource>();
    block.add(fromResource);
    unrelatedBlocks.add(block);
    return block;
  }

  private Set<AsmResource> getResourceBlock(AsmResource fromResource) {
    for (Set<AsmResource> block : unrelatedBlocks) {
      if (block.contains(fromResource)) {
        return block;
      }
    }
    return null;
  }
}
