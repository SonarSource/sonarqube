/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.api.posttask;

import java.util.Set;
import java.util.function.Function;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.QualityGate.EvaluationStatus;
import org.sonar.api.measures.Metric;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.qualitygate.Condition;
import org.sonar.server.qualitygate.EvaluatedCondition;
import org.sonar.server.qualitygate.EvaluatedQualityGate;

public class QGToEvaluatedQG implements Function<QualityGate, EvaluatedQualityGate> {
  public static final QGToEvaluatedQG INSTANCE = new QGToEvaluatedQG();

  private QGToEvaluatedQG() {
    // static only
  }

  @Override public EvaluatedQualityGate apply(QualityGate qg) {
    EvaluatedQualityGate.Builder builder = EvaluatedQualityGate.newBuilder();
    Set<Condition> conditions = qg.getConditions().stream()
      .map(q -> {
        Condition condition = new Condition(q.getMetricKey(), Condition.Operator.valueOf(q.getOperator().name()),
          q.getErrorThreshold());
        builder.addEvaluatedCondition(condition,
          EvaluatedCondition.EvaluationStatus.valueOf(q.getStatus().name()),
          q.getStatus() == EvaluationStatus.NO_VALUE ? null : q.getValue());
        return condition;
      })
      .collect(MoreCollectors.toSet());
    return builder.setQualityGate(new org.sonar.server.qualitygate.QualityGate(qg.getId(), qg.getName(), conditions))
      .setStatus(Metric.Level.valueOf(qg.getStatus().name()))
      .build();
  }
}
