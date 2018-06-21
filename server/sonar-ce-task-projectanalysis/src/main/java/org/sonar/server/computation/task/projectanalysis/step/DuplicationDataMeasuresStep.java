/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.step;

import org.apache.commons.lang.StringEscapeUtils;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.duplication.CrossProjectDuplicate;
import org.sonar.server.computation.task.projectanalysis.duplication.Duplicate;
import org.sonar.server.computation.task.projectanalysis.duplication.Duplication;
import org.sonar.server.computation.task.projectanalysis.duplication.DuplicationRepository;
import org.sonar.server.computation.task.projectanalysis.duplication.InProjectDuplicate;
import org.sonar.server.computation.task.projectanalysis.duplication.InnerDuplicate;
import org.sonar.server.computation.task.projectanalysis.duplication.TextBlock;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.step.ComputationStep;

import static com.google.common.collect.Iterables.isEmpty;
import static org.sonar.api.measures.CoreMetrics.DUPLICATIONS_DATA_KEY;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * Compute duplication data measures on files, based on the {@link DuplicationRepository}
 */
public class DuplicationDataMeasuresStep implements ComputationStep {

  private final MeasureRepository measureRepository;
  private final TreeRootHolder treeRootHolder;
  private final DuplicationRepository duplicationRepository;

  private final Metric duplicationDataMetric;

  public DuplicationDataMeasuresStep(TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository,
    DuplicationRepository duplicationRepository) {
    this.measureRepository = measureRepository;
    this.treeRootHolder = treeRootHolder;
    this.duplicationRepository = duplicationRepository;
    this.duplicationDataMetric = metricRepository.getByKey(DUPLICATIONS_DATA_KEY);
  }

  @Override
  public void execute() {
    new DepthTraversalTypeAwareCrawler(new DuplicationVisitor())
      .visit(treeRootHolder.getRoot());
  }

  private class DuplicationVisitor extends TypeAwareVisitorAdapter {

    private DuplicationVisitor() {
      super(CrawlerDepthLimit.FILE, PRE_ORDER);
    }

    @Override
    public void visitFile(Component file) {
      Iterable<Duplication> duplications = duplicationRepository.getDuplications(file);
      if (!isEmpty(duplications)) {
        computeDuplications(file, duplications);
      }
    }

    private void computeDuplications(Component component, Iterable<Duplication> duplications) {
      String duplicationXml = createXmlDuplications(component.getKey(), duplications);
      measureRepository.add(
        component,
        duplicationDataMetric,
        Measure.newMeasureBuilder().create(duplicationXml)
        );
    }

    private String createXmlDuplications(String componentKey, Iterable<Duplication> duplications) {
      StringBuilder xml = new StringBuilder();
      xml.append("<duplications>");
      for (Duplication duplication : duplications) {
        xml.append("<g>");
        appendDuplication(xml, componentKey, duplication.getOriginal());
        for (Duplicate duplicate : duplication.getDuplicates()) {
          processDuplicationBlock(xml, duplicate, componentKey);
        }
        xml.append("</g>");
      }
      xml.append("</duplications>");
      return xml.toString();
    }

    private void processDuplicationBlock(StringBuilder xml, Duplicate duplicate, String componentKey) {
      if (duplicate instanceof InnerDuplicate) {
        // Duplication is on a the same file
        appendDuplication(xml, componentKey, duplicate);
      } else if (duplicate instanceof InProjectDuplicate) {
        // Duplication is on a different file
        appendDuplication(xml, ((InProjectDuplicate) duplicate).getFile().getKey(), duplicate);
      } else if (duplicate instanceof CrossProjectDuplicate) {
        // componentKey is only set for cross project duplications
        String crossProjectComponentKey = ((CrossProjectDuplicate) duplicate).getFileKey();
        appendDuplication(xml, crossProjectComponentKey, duplicate);
      } else {
        throw new IllegalArgumentException("Unsupported type of Duplicate " + duplicate.getClass().getName());
      }
    }

    private void appendDuplication(StringBuilder xml, String componentKey, Duplicate duplicate) {
      appendDuplication(xml, componentKey, duplicate.getTextBlock());
    }

    private void appendDuplication(StringBuilder xml, String componentKey, TextBlock textBlock) {
      int length = textBlock.getEnd() - textBlock.getStart() + 1;
      xml.append("<b s=\"").append(textBlock.getStart())
        .append("\" l=\"").append(length)
        .append("\" r=\"").append(StringEscapeUtils.escapeXml(componentKey))
        .append("\"/>");
    }
  }

  @Override
  public String getDescription() {
    return "Compute duplication data measures";
  }

}
