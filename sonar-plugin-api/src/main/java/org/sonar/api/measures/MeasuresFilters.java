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
package org.sonar.api.measures;

import org.sonar.api.qualitymodel.Characteristic;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @since 1.10
 */
public final class MeasuresFilters {

  private MeasuresFilters() {
  }

  public static MeasuresFilter<Collection<Measure>> all() {
    return new MeasuresFilter<Collection<Measure>>() {
      public Collection<Measure> filter(Collection<Measure> measures) {
        return measures;
      }
    };
  }

  public static MeasuresFilter<Measure> metric(final Metric metric) {
    return new MetricFilter<Measure>(metric) {

      public Measure filter(Collection<Measure> measures) {
        if (measures == null) {
          return null;
        }
        for (Measure measure : measures) {
          if (measure.getClass().equals(Measure.class) &&
              measure.getMetric().equals(metric) &&
              measure.getCharacteristic()==null) {
            return measure;
          }
        }
        return null;
      }
    };
  }

  public static MeasuresFilter<Measure> characteristic(final Metric metric, final Characteristic characteristic) {
    return new MetricFilter<Measure>(metric) {

      public Measure filter(Collection<Measure> measures) {
        if (measures == null) {
          return null;
        }
        for (Measure measure : measures) {
          if (measure.getClass().equals(Measure.class) &&
              measure.getMetric().equals(metric) &&
              measure.getCharacteristic()!=null &&
              measure.getCharacteristic().equals(characteristic)) {
            return measure;
          }
        }
        return null;
      }
    };
  }
  
  /**
   * @since 2.0
   */
  public static MeasuresFilter<Measure> measure(final Measure measure) {
    return new MeasuresFilter<Measure>() {
      public Measure filter(Collection<Measure> measures) {
        if (measures==null) {
          return null;
        }
        for (Measure m : measures) {
          if (m.equals(measure)) {
            return m;
          }
        }
        return null;
      }
    };
  }

  public static MeasuresFilter<RuleMeasure> rulePriority(final Metric metric, final RulePriority priority) {
    return new RulePriorityFilter(metric, priority);
  }

  public static MeasuresFilter<RuleMeasure> ruleCategory(final Metric metric, final Integer category) {
    return new RuleCategoryFilter(metric, category);
  }

  public static MeasuresFilter<RuleMeasure> rule(final Metric metric, final Rule rule) {
    return new RuleFilter(metric, rule);
  }

  public static MeasuresFilter<Collection<RuleMeasure>> ruleCategories(final Metric metric) {
    return new MetricFilter<Collection<RuleMeasure>>(metric) {

      private boolean apply(Measure measure) {
        return measure instanceof RuleMeasure
            && metric.equals(measure.getMetric())
            && ((RuleMeasure) measure).getRule() == null
            && ((RuleMeasure) measure).getRuleCategory() != null;
      }

      public Collection<RuleMeasure> filter(Collection<Measure> measures) {
        List<RuleMeasure> result = new ArrayList<RuleMeasure>();
        if (measures != null) {
          for (Measure measure : measures) {
            if (apply(measure)) {
              result.add((RuleMeasure) measure);
            }
          }
        }
        return result;
      }
    };
  }

  public static MeasuresFilter<Collection<RuleMeasure>> rules(final Metric metric) {
    return new MetricFilter<Collection<RuleMeasure>>(metric) {

      private boolean apply(Measure measure) {
        return measure instanceof RuleMeasure
            && metric.equals(measure.getMetric())
            && ((RuleMeasure) measure).getRule() != null;
      }

      public Collection<RuleMeasure> filter(Collection<Measure> measures) {
        if (measures == null) {
          return null;
        }
        List<RuleMeasure> result = new ArrayList<RuleMeasure>();
        for (Measure measure : measures) {
          if (apply(measure)) {
            result.add((RuleMeasure) measure);
          }
        }
        return result;
      }
    };
  }

  /**
   * Used for internal optimizations.
   */
  public static abstract class MetricFilter<M> implements MeasuresFilter<M> {
    private final Metric metric;

    protected MetricFilter(Metric metric) {
      this.metric = metric;
    }

    public Metric filterOnMetric() {
      return metric;
    }
  }

  private abstract static class AbstractRuleMeasureFilter<M> extends MetricFilter<M> {
    protected AbstractRuleMeasureFilter(Metric metric) {
      super(metric);
    }

    private boolean apply(Measure measure) {
      return measure instanceof RuleMeasure
          && filterOnMetric().equals(measure.getMetric())
          && doApply((RuleMeasure) measure);
    }

    abstract boolean doApply(RuleMeasure ruleMeasure);

    public M filter(Collection<Measure> measures) {
      if (measures == null) {
        return null;
      }
      for (Measure measure : measures) {
        if (apply(measure)) {
          return (M) measure;
        }
      }
      return null;
    }
  }

  private static class RulePriorityFilter extends AbstractRuleMeasureFilter<RuleMeasure> {
    private RulePriority priority;

    protected RulePriorityFilter(Metric metric, RulePriority priority) {
      super(metric);
      this.priority = priority;
    }

    @Override
    boolean doApply(RuleMeasure measure) {
      return measure.getRule() == null
          && measure.getRuleCategory() == null
          && priority.equals(measure.getRulePriority());
    }
  }

  private static class RuleCategoryFilter extends AbstractRuleMeasureFilter<RuleMeasure> {
    private Integer categ;

    protected RuleCategoryFilter(Metric metric, Integer categ) {
      super(metric);
      this.categ = categ;
    }

    @Override
    boolean doApply(RuleMeasure measure) {
      return measure.getRule() == null
          && categ.equals(measure.getRuleCategory())
          && measure.getRulePriority() == null;
    }
  }


  private static class RuleFilter extends AbstractRuleMeasureFilter<RuleMeasure> {
    private Rule rule;

    protected RuleFilter(Metric metric, Rule rule) {
      super(metric);
      this.rule = rule;
    }

    @Override
    boolean doApply(RuleMeasure measure) {
      return measure.getRule() != null
          && rule.equals(measure.getRule());
    }
  }
}
