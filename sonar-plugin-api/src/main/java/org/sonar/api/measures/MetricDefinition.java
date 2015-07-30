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

package org.sonar.api.measures;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.config.Settings;

/**
 * TODO
 *
 *
 */
public interface MetricDefinition {

  class NewMetricContext {
    private final Map<String, Metric> metricsByKey = Maps.newHashMap();

    public NewMetric createNewMetric() {
      return new NewMetricImpl(this);
    }

    @CheckForNull
    public Metric getMetric(String metricKey) {
      return metricsByKey.get(metricKey);
    }

    public List<Metric> getMetrics(){
      return ImmutableList.copyOf(metricsByKey.values());
    }

    private void registerMetric(NewMetricImpl newMetric) {
      // TODO check metric does not already exists
      metricsByKey.put(newMetric.key, new MetricImpl(newMetric));
    }
  }

  interface NewMetric {
    NewMetric setKey(String s);

    NewMetric setName(String s);

    // TODO add all fields needed on metric : domain, type, best value, direction, qualitative, etc.

    NewComputer createMeasureComputer();

    void done();
  }

  class NewMetricImpl implements NewMetric {
    private final NewMetricContext context;

    private String key;

    private String name;

    private Computer computer;

    public NewMetricImpl(NewMetricContext context) {
      this.context = context;
    }

    @Override
    public NewMetric setKey(String s) {
      this.key = s;
      return this;
    }

    @Override
    public NewMetric setName(String s) {
      this.name = s;
      return this;
    }

    @Override
    public NewComputer createMeasureComputer() {
      return new NewComputerImpl(this);
    }

    void setNewComputerImpl(NewComputerImpl newComputer){
      this.computer = new ComputerImpl(newComputer);
    }

    @Override
    public void done() {
      // TODO fail if no computer
      context.registerMetric(this);
    }
  }

  interface Metric {
    String getKey();

    String getName();

    Computer getComputer();
  }

  class MetricImpl implements Metric {
    private final String key;
    private final String name;
    private final Computer computer;

    public MetricImpl(NewMetricImpl newMetric) {
      this.key = newMetric.key;
      this.name = newMetric.name;
      this.computer = newMetric.computer;
    }

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Computer getComputer() {
      return computer;
    }

    @Override
    public String toString() {
      return "Metric{" +
        "key='" + key + '\'' +
        '}';
    }
  }

  interface NewComputer {
    NewComputer setInputMetricKeys(String... metricKeys);

    NewComputer setMeasureComputer(MeasureComputer measureComputer);

    NewMetric done();
  }

  class NewComputerImpl implements NewComputer {

    private final NewMetricImpl newMetric;

    public NewComputerImpl(NewMetricImpl newMetric) {
      this.newMetric = newMetric;
    }

    private String[] inputMetricKeys;
    private MeasureComputer measureComputer;

    @Override
    public NewComputer setInputMetricKeys(String... metricKeys) {
      this.inputMetricKeys = metricKeys;
      return this;
    }

    @Override
    public NewComputer setMeasureComputer(MeasureComputer measureComputer) {
      this.measureComputer = measureComputer;
      return this;
    }

    @Override
    public NewMetric done() {
      // TODO fail if no metrics or no computer
      newMetric.setNewComputerImpl(this);
      return newMetric;
    }
  }

  interface MeasureComputer {
    void compute(MeasureComputerContext context);
  }

  interface MeasureComputerContext {
    Settings getSettings();

    Component getComponent();

    @CheckForNull
    Measure getMeasure(String metric);

    List<Measure> getChildrenMeasures(String metric);

    void saveMeasure(int value);

    void saveMeasure(double value);

    void saveMeasure(long value);

    void saveMeasure(String value);
  }

  interface Measure {
    int getIntValue();

    long getLongValue();

    double getDoubleValue();

    String getStringValue();
  }

  interface Component {
    enum Type {
      PROJECT, MODULE, DIRECTORY, FILE
    }

    Type getType();

    boolean isUnitTest();
  }

  interface Computer {
    String[] getInputMetricKeys();

    MeasureComputer getMeasureComputer();
  }

  class ComputerImpl implements Computer {

    private final String[] inputMetricKeys;
    private final MeasureComputer measureComputer;

    public ComputerImpl(NewComputerImpl newComputer) {
      this.inputMetricKeys = newComputer.inputMetricKeys;
      this.measureComputer = newComputer.measureComputer;
    }

    @Override
    public String[] getInputMetricKeys() {
      return inputMetricKeys;
    }

    @Override
    public MeasureComputer getMeasureComputer() {
      return measureComputer;
    }
  }

  void define(NewMetricContext newMetricContext);

}
