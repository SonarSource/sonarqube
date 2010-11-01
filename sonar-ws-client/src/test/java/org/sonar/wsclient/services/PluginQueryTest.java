package org.sonar.wsclient.services;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PluginQueryTest {

  @Test
  public void index() {
    PluginQuery query = new PluginQuery();
    assertThat(query.getUrl(), is("/api/plugins"));
  }

}
