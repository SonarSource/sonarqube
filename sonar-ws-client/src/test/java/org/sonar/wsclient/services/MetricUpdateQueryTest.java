package org.sonar.wsclient.services;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class MetricUpdateQueryTest extends QueryTestCase {

  @Test
  public void should_update() {
    MetricUpdateQuery query = MetricUpdateQuery.update("key").setName("name").setDescription("description").setDomain("domain").setType("type");
    assertThat(query.getUrl()).isEqualTo("/api/metrics/key?name=name&description=description&domain=domain&val_type=type&");
    assertThat(query.getBody()).isEqualTo("description");
    assertThat(query.getModelClass().getName()).isEqualTo(Metric.class.getName());
  }

}
