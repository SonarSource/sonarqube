package org.sonar.server.platform.cluster;

public class ClusterMock implements Cluster {

  private boolean enabled = false;
  private boolean startupLeader = false;

  public ClusterMock setEnabled(boolean b) {
    this.enabled = b;
    return this;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public ClusterMock setStartupLeader(boolean b) {
    this.startupLeader = b;
    return this;
  }

  @Override
  public boolean isStartupLeader() {
    return startupLeader;
  }
}
