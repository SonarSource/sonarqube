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
package org.sonar.plugins.squid.bridges;

import com.google.common.collect.Lists;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.java.bytecode.asm.AsmField;
import org.sonar.java.bytecode.asm.AsmMethod;
import org.sonar.java.bytecode.asm.AsmResource;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.measures.Metric;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class Lcom4BlocksBridge extends Bridge {

  protected Lcom4BlocksBridge() {
    super(true);
  }

  @Override
  public void onFile(SourceFile squidFile, Resource sonarFile) {
    List<Set<AsmResource>> blocks = (List<Set<AsmResource>>) squidFile.getData(Metric.LCOM4_BLOCKS);

    // This measure includes AsmResource objects and it is used only by this bridge, so
    // it can be removed from memory.
    squidFile.removeMeasure(Metric.LCOM4_BLOCKS);

    if (blocks != null && !blocks.isEmpty()) {
      Measure measure = new Measure(CoreMetrics.LCOM4_BLOCKS, serialize(blocks));
      measure.setPersistenceMode(PersistenceMode.DATABASE);
      context.saveMeasure(sonarFile, measure);
    }
  }

  protected void sortBlocks(List<Set<AsmResource>> blocks) {
    Collections.sort(blocks, new Comparator<Set>() {
      public int compare(Set set1, Set set2) {
        return set1.size() - set2.size();
      }
    });
  }

  protected String serialize(List<Set<AsmResource>> blocks) {
    sortBlocks(blocks);

    StringBuilder sb = new StringBuilder();
    sb.append('[');

    for (int indexBlock = 0; indexBlock < blocks.size(); indexBlock++) {
      blocks.get(indexBlock);
      Set<AsmResource> block = blocks.get(indexBlock);
      if (!block.isEmpty()) {
        if (indexBlock > 0) {
          sb.append(',');
        }
        sb.append('[');
        serializeBlock(block, sb);
        sb.append(']');
      }
    }
    sb.append(']');
    return sb.toString();
  }

  private void serializeBlock(Set<AsmResource> block, StringBuilder sb) {
    List<AsmResource> sortedResources = sortResourcesInBlock(block);
    int indexResource = 0;
    for (AsmResource resource : sortedResources) {
      if (indexResource++ > 0) {
        sb.append(',');
      }
      serializeResource(resource, sb);
    }
  }

  private void serializeResource(AsmResource resource, StringBuilder sb) {
    sb.append("{\"q\":\"");
    sb.append(toQualifier(resource));
    sb.append("\",\"n\":\"");
    sb.append(resource.toString());
    sb.append("\"}");
  }

  protected List<AsmResource> sortResourcesInBlock(Set<AsmResource> block) {
    List<AsmResource> result = Lists.newArrayList();
    result.addAll(block);

    Collections.sort(result, new Comparator<AsmResource>() {
      public int compare(AsmResource asmResource1, AsmResource asmResource2) {
        int result = compareType(asmResource1, asmResource2);
        if (result == 0) {
          result = asmResource1.toString().compareTo(asmResource2.toString());
        }
        return result;
      }

      private int compareType(AsmResource asmResource1, AsmResource asmResource2) {
        if (asmResource1 instanceof AsmField) {
          return (asmResource2 instanceof AsmField ? 0 : -1);
        }
        return (asmResource2 instanceof AsmMethod ? 0 : 1);
      }
    });

    return result;
  }

  private static String toQualifier(AsmResource asmResource) {
    if (asmResource instanceof AsmField) {
      return Qualifiers.FIELD;
    }
    if (asmResource instanceof AsmMethod) {
      return Qualifiers.METHOD;
    }
    throw new IllegalArgumentException("Wrong ASM resource: " + asmResource.getClass());
  }
}
