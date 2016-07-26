package org.sonar.server.platform.cluster;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

public class ClusterProperties {

  public static final String ENABLED = "sonar.cluster.enabled";
  public static final String STARTUP_LEADER = "sonar.cluster.startupLeader";

  public static List<PropertyDefinition> definitions() {
    return ImmutableList.of(
      PropertyDefinition.builder(ENABLED)
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(false))
        .hidden()
        .build(),

      PropertyDefinition.builder(STARTUP_LEADER)
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(false))
        .hidden()
        .build());
  }
}
