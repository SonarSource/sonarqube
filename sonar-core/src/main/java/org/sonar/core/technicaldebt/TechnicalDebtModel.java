/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.technicaldebt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.qualitymodel.Model;
import org.sonar.api.qualitymodel.ModelFinder;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TimeProfiler;

import javax.annotation.CheckForNull;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

public class TechnicalDebtModel implements BatchComponent {

  private static final Logger LOGGER = LoggerFactory.getLogger(TechnicalDebtModel.class);

  // FIXME Use the same as in RegisterTechnicalDebtModel
  public static final String MODEL_NAME = "TECHNICAL_DEBT";

  private List<TechnicalDebtCharacteristic> characteristics = newArrayList();
  private Map<Rule, TechnicalDebtRequirement> requirementsByRule = newHashMap();

  public TechnicalDebtModel(ModelFinder modelFinder) {
    TimeProfiler profiler = new TimeProfiler(LOGGER).start("Loading technical debt model");
    Model model = modelFinder.findByName(MODEL_NAME);
    if (model == null) {
      throw new SonarException("Can not find the model in database: " + MODEL_NAME);
    }
    init(model);
    profiler.stop();
  }

  /**
   * For unit tests
   */
  private TechnicalDebtModel(Model model) {
    init(model);
  }

  /**
   * For unit tests
   */
  public static TechnicalDebtModel create(Model model) {
    return new TechnicalDebtModel(model);
  }

  private void init(Model model) {
    for (org.sonar.api.qualitymodel.Characteristic characteristic : model.getRootCharacteristics()) {
      if (characteristic.getEnabled()) {
        TechnicalDebtCharacteristic sc = new TechnicalDebtCharacteristic(characteristic);
        characteristics.add(sc);
        registerRequirements(sc);
      }
    }
  }

  private void registerRequirements(TechnicalDebtCharacteristic c) {
    for (TechnicalDebtRequirement requirement : c.getRequirements()) {

      if (requirement.getRule() != null) {
        requirementsByRule.put(requirement.getRule(), requirement);
      }
    }
    for (TechnicalDebtCharacteristic subCharacteristic : c.getSubCharacteristics()) {
      registerRequirements(subCharacteristic);
    }
  }

  public List<TechnicalDebtCharacteristic> getCharacteristics() {
    return characteristics;
  }

  public Collection<TechnicalDebtRequirement> getAllRequirements() {
    return requirementsByRule.values();
  }

  @CheckForNull
  public TechnicalDebtRequirement getRequirementByRule(String repositoryKey, String key) {
    return requirementsByRule.get(Rule.create().setUniqueKey(repositoryKey, key));
  }
}
