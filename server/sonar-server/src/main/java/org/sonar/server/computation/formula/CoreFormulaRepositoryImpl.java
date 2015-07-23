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
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS_KEY;
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

    new SumFormula(ACCESSORS_KEY)
    );

  /**
   * Return list of formulas that was previously provided by CoreMetrics
   */
  @Override
  public List<Formula> getFormulas() {
    return FORMULAS;
  }

}
