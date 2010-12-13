package org.sonar.wsclient.unmarshallers;

import org.junit.Test;
import org.sonar.wsclient.services.Plugin;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PluginUnmarshallerTest {

  @Test
  public void toModel() throws Exception {
    List<Plugin> plugins = new PluginUnmarshaller().toModels(WSTestUtils.loadFile("/plugins/plugins.json"));
    Plugin plugin = plugins.get(0);
    assertThat(plugin.getKey(), is("foo"));
    assertThat(plugin.getName(), is("Foo"));
    assertThat(plugin.getVersion(), is("1.0"));
  }

}
