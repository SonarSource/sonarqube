/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.formula;

import com.google.common.collect.ImmutableList;
import java.util.List;

import static org.sonar.api.measures.CoreMetrics.ACCESSORS_KEY;
import static org.sonar.api.measures.CoreMetrics.CLASSES_KEY;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.COMMENTED_OUT_CODE_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_CLASSES_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.FILES_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_KEY;
import static org.sonar.api.measures.CoreMetrics.GENERATED_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.GENERATED_NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.PACKAGES_KEY;
import static org.sonar.api.measures.CoreMetrics.STATEMENTS_KEY;

public class CoreFormulaRepositoryImpl implements CoreFormulaRepository {

  private static final List<Formula> FORMULAS = ImmutableList.<Formula>of(
    // TODO When all decorators will be moved to CE, uncomment commented lines to activate all formulas and remove formulas declaration in
    // {@link org.sonar.api.measures.CoreMetrics}

    // Sum formulas
    // new SumFormula(LINES_KEY),
    new SumFormula(GENERATED_LINES_KEY),
    // new SumFormula(NCLOC_KEY),
    new SumFormula(GENERATED_NCLOC_KEY),
    new SumFormula(CLASSES_KEY),
    new SumFormula(FUNCTIONS_KEY),
    new SumFormula(STATEMENTS_KEY),
    new SumFormula(PACKAGES_KEY),
    new SumFormula(COMMENTED_OUT_CODE_LINES_KEY),
    new SumFormula(COMPLEXITY_KEY),
    new SumFormula(COMPLEXITY_IN_CLASSES_KEY),
    // TODO this formula seems to be useless as this measure seems only required on files
    new SumFormula(COMPLEXITY_IN_FUNCTIONS_KEY),

    new SumFormula(ACCESSORS_KEY),
//    new SumFormula(CoreMetrics.COMMENT_LINES_KEY),

    // Distribution formulas
    new DistributionFormula(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY),
    new DistributionFormula(FILE_COMPLEXITY_DISTRIBUTION_KEY),

    // Average formulas, must be executed after all sum formulas as they depend on their measures
    AverageFormula.Builder.newBuilder().setOutputMetricKey(FILE_COMPLEXITY_KEY)
      .setMainMetricKey(COMPLEXITY_KEY)
      .setByMetricKey(FILES_KEY)
      .build(),
    AverageFormula.Builder.newBuilder().setOutputMetricKey(CLASS_COMPLEXITY_KEY)
      .setMainMetricKey(COMPLEXITY_IN_CLASSES_KEY)
      .setByMetricKey(CLASSES_KEY)
      .setFallbackMetricKey(COMPLEXITY_KEY)
      .build(),
    AverageFormula.Builder.newBuilder().setOutputMetricKey(FUNCTION_COMPLEXITY_KEY)
      .setMainMetricKey(COMPLEXITY_IN_FUNCTIONS_KEY)
      .setByMetricKey(FUNCTIONS_KEY)
      .setFallbackMetricKey(COMPLEXITY_KEY)
      .build()
  );

  /**
   * Return list of formulas that was previously provided by CoreMetrics
   */
  @Override
  public List<Formula> getFormulas() {
    return FORMULAS;
  }

}
