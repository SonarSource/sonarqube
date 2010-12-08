package org.sonar.core.components;

import com.google.common.collect.Lists;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.util.Collection;
import java.util.List;

public class DefaultMetricFinder implements MetricFinder {

  private DatabaseSessionFactory sessionFactory;

  public DefaultMetricFinder(DatabaseSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public Metric findById(int id) {
    return sessionFactory.getSession().getSingleResult(Metric.class, "id", id, "enabled", true);
  }

  public Metric findByKey(String key) {
    return sessionFactory.getSession().getSingleResult(Metric.class, "key", key, "enabled", true);
  }

  public Collection<Metric> findAll(List<String> metricKeys) {
    List<Metric> result = Lists.newLinkedList();
    for (String metricKey : metricKeys) {
      Metric metric = findByKey(metricKey);
      if (metric != null) {
        result.add(metric);
      }
    }
    return result;
  }

  public Collection<Metric> findAll() {
    return doFindAll();
  }

  protected Collection<Metric> doFindAll() {
    return sessionFactory.getSession().getResults(Metric.class, "enabled", true);
  }

}
