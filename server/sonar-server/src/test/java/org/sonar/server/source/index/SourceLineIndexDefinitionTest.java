package org.sonar.server.source.index;

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.process.ProcessConstants;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.NewIndex;

import static org.fest.assertions.Assertions.assertThat;

public class SourceLineIndexDefinitionTest {

  IndexDefinition.IndexDefinitionContext context = new IndexDefinition.IndexDefinitionContext();

  @Test
  public void define() throws Exception {
    IndexDefinition def = new SourceLineIndexDefinition(new Settings());
    def.define(context);

    assertThat(context.getIndices()).hasSize(1);
    NewIndex index = context.getIndices().get("sourcelines");
    assertThat(index).isNotNull();
    assertThat(index.getTypes().keySet()).containsOnly("sourceline");

    // no cluster by default
    assertThat(index.getSettings().get("index.number_of_shards")).isEqualTo("1");
    assertThat(index.getSettings().get("index.number_of_replicas")).isEqualTo("0");
  }

  @Test
  public void enable_cluster() throws Exception {
    Settings settings = new Settings();
    settings.setProperty(ProcessConstants.CLUSTER_ACTIVATE, true);
    IndexDefinition def = new SourceLineIndexDefinition(settings);
    def.define(context);

    NewIndex issuesIndex = context.getIndices().get("sourcelines");
    assertThat(issuesIndex.getSettings().get("index.number_of_shards")).isEqualTo("4");
    assertThat(issuesIndex.getSettings().get("index.number_of_replicas")).isEqualTo("1");
  }

}
