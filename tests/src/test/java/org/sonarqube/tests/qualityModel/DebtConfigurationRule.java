/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.tests.qualityModel;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.sonar.orchestrator.Orchestrator;
import java.util.Set;
import org.junit.rules.ExternalResource;

import static com.google.common.base.Preconditions.checkState;
import static util.ItUtils.setServerProperty;

/**
 * This rule should be used when dealing with technical debt properties, in order to always be sure that the properties are correctly reset between each tests.
 */
public class DebtConfigurationRule extends ExternalResource {

  private static final String DEV_COST_PROPERTY = "sonar.technicalDebt.developmentCost";
  private static final String RATING_GRID_PROPERTY = "sonar.technicalDebt.ratingGrid";

  private static final String DEV_COST_LANGUAGE_PROPERTY = "languageSpecificParameters";
  private static final String DEV_COST_LANGUAGE_NAME_PROPERTY = DEV_COST_LANGUAGE_PROPERTY + ".0.language";
  private static final String DEV_COST_LANGUAGE_COST_PROPERTY = DEV_COST_LANGUAGE_PROPERTY + ".0.man_days";

  private static final Joiner COMA_JOINER = Joiner.on(",");

  private static final Set<String> DEV_COST_PROPERTIES = ImmutableSet.of(
    DEV_COST_PROPERTY,
    DEV_COST_LANGUAGE_PROPERTY,
    DEV_COST_LANGUAGE_NAME_PROPERTY,
    DEV_COST_LANGUAGE_COST_PROPERTY,
    RATING_GRID_PROPERTY);

  private final Orchestrator orchestrator;

  private DebtConfigurationRule(Orchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  public static DebtConfigurationRule create(Orchestrator orchestrator) {
    return new DebtConfigurationRule(orchestrator);
  }

  @Override
  protected void before() throws Throwable {
    reset();
  }

  @Override
  protected void after() {
    reset();
  }

  public void reset() {
    resetDevelopmentCost();
    resetRatingGrid();
  }

  public DebtConfigurationRule updateDevelopmentCost(int developmentCost) {
    setProperty(DEV_COST_PROPERTY, Integer.toString(developmentCost));
    return this;
  }

  public DebtConfigurationRule updateLanguageDevelopmentCost(String language, int developmentCost) {
    setServerProperty(orchestrator, DEV_COST_LANGUAGE_PROPERTY, "0");
    setServerProperty(orchestrator, DEV_COST_LANGUAGE_NAME_PROPERTY, language);
    setServerProperty(orchestrator, DEV_COST_LANGUAGE_COST_PROPERTY, Integer.toString(developmentCost));
    return this;
  }

  public void resetDevelopmentCost() {
    for (String property : DEV_COST_PROPERTIES) {
      resetProperty(property);
    }
  }

  public DebtConfigurationRule updateRatingGrid(Double... ratingGrid) {
    checkState(ratingGrid.length == 4, "Rating grid must contains 4 values");
    setProperty(RATING_GRID_PROPERTY, COMA_JOINER.join(ratingGrid));
    return this;
  }

  public DebtConfigurationRule resetRatingGrid() {
    resetProperty(RATING_GRID_PROPERTY);
    return this;
  }

  private void setProperty(String property, String value) {
    setServerProperty(orchestrator, property, value);
  }

  private void resetProperty(String property) {
    setProperty(property, null);
  }

}
