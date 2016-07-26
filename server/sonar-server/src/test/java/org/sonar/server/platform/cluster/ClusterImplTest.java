package org.sonar.server.platform.cluster;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;

import static org.assertj.core.api.Assertions.assertThat;

public class ClusterImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Settings settings = new Settings(new PropertyDefinitions(ClusterProperties.definitions()));

  @Test
  public void cluster_is_disabled_by_default() {
    ClusterImpl underTest = new ClusterImpl(settings);

    assertThat(underTest.isEnabled()).isFalse();
    assertThat(underTest.isStartupLeader()).isTrue();
  }

  @Test
  public void node_is_startup_leader_in_cluster() {
    settings.setProperty("sonar.cluster.enabled", "true");
    settings.setProperty("sonar.cluster.startupLeader", "true");

    ClusterImpl underTest = new ClusterImpl(settings);

    assertThat(underTest.isEnabled()).isTrue();
    assertThat(underTest.isStartupLeader()).isTrue();
  }

  @Test
  public void node_is_startup_follower_by_default_in_cluster() {
    settings.setProperty("sonar.cluster.enabled", "true");

    ClusterImpl underTest = new ClusterImpl(settings);

    assertThat(underTest.isEnabled()).isTrue();
    assertThat(underTest.isStartupLeader()).isFalse();
  }

  @Test
  public void node_is_startup_follower_in_cluster() {
    settings.setProperty("sonar.cluster.enabled", "true");
    settings.setProperty("sonar.cluster.startupLeader", "false");

    ClusterImpl underTest = new ClusterImpl(settings);

    assertThat(underTest.isEnabled()).isTrue();
    assertThat(underTest.isStartupLeader()).isFalse();
  }

}
