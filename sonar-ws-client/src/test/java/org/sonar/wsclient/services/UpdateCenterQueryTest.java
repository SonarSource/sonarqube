package org.sonar.wsclient.services;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class UpdateCenterQueryTest {

  @Test
  public void index() {
    UpdateCenterQuery query = UpdateCenterQuery.createForInstalledPlugins();
    assertThat(query.getUrl(), is("/api/updatecenter/installed_plugins"));
  }

}
