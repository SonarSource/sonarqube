/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.ce.task.projectanalysis.duplication;

import java.util.Optional;
import org.apache.commons.text.StringEscapeUtils;
import org.sonar.ce.task.projectanalysis.component.Component;

import static com.google.common.collect.Iterables.isEmpty;

/**
 * Compute duplication data measures on files, based on the {@link DuplicationRepository}
 */
public class ComputeDuplicationDataMeasure {
  private final DuplicationRepository duplicationRepository;

  public ComputeDuplicationDataMeasure(DuplicationRepository duplicationRepository) {
    this.duplicationRepository = duplicationRepository;
  }

  public Optional<String> compute(Component file) {
    Iterable<Duplication> duplications = duplicationRepository.getDuplications(file);
    if (isEmpty(duplications)) {
      return Optional.empty();
    }
    return Optional.of(generateMeasure(file.getKey(), duplications));
  }

  private static String generateMeasure(String componentDbKey, Iterable<Duplication> duplications) {
    StringBuilder xml = new StringBuilder();
    xml.append("<duplications>");
    for (Duplication duplication : duplications) {
      xml.append("<g>");
      appendDuplication(xml, componentDbKey, duplication.getOriginal(), false);
      for (Duplicate duplicate : duplication.getDuplicates()) {
        processDuplicationBlock(xml, duplicate, componentDbKey);
      }
      xml.append("</g>");
    }
    xml.append("</duplications>");
    return xml.toString();
  }

  private static void processDuplicationBlock(StringBuilder xml, Duplicate duplicate, String componentDbKey) {
    if (duplicate instanceof InnerDuplicate) {
      // Duplication is on the same file
      appendDuplication(xml, componentDbKey, duplicate);
    } else if (duplicate instanceof InExtendedProjectDuplicate inExtendedProjectDuplicate) {
      // Duplication is on a different file that is not saved in the DB
      appendDuplication(xml, inExtendedProjectDuplicate.getFile().getKey(), duplicate.getTextBlock(), true);
    } else if (duplicate instanceof InProjectDuplicate inProjectDuplicate) {
      // Duplication is on a different file
      appendDuplication(xml, inProjectDuplicate.getFile().getKey(), duplicate);
    } else if (duplicate instanceof CrossProjectDuplicate crossProjectDuplicate) {
      // Only componentKey is set for cross project duplications
      String crossProjectComponentKey = crossProjectDuplicate.getFileKey();
      appendDuplication(xml, crossProjectComponentKey, duplicate);
    } else {
      throw new IllegalArgumentException("Unsupported type of Duplicate " + duplicate.getClass().getName());
    }
  }

  private static void appendDuplication(StringBuilder xml, String componentDbKey, Duplicate duplicate) {
    appendDuplication(xml, componentDbKey, duplicate.getTextBlock(), false);
  }

  private static void appendDuplication(StringBuilder xml, String componentDbKey, TextBlock textBlock, boolean disableLink) {
    int length = textBlock.getEnd() - textBlock.getStart() + 1;
    xml.append("<b s=\"").append(textBlock.getStart())
      .append("\" l=\"").append(length)
      .append("\" t=\"").append(disableLink)
      .append("\" r=\"").append(StringEscapeUtils.escapeXml10(componentDbKey))
      .append("\"/>");
  }
}
