package org.sonar.plugins.findbugs;

import org.junit.Test;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenUtils;
import org.sonar.api.resources.Project;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class FindbugsConfigurationTest {
  @Test
  public void shouldReuseExistingRulesConfig() throws Exception {
    Project project = mock(Project.class);
    when(project.getReuseExistingRulesConfig()).thenReturn(true);

    MavenPlugin plugin = new MavenPlugin(MavenUtils.GROUP_ID_CODEHAUS_MOJO, "findbugs-maven-plugin", "2.3.1");
    plugin.setParameter("excludeFilterFile", "rules/exclude.xml");
    plugin.setParameter("includeFilterFile", "rules/include.xml");

    FindbugsConfiguration conf = spy(new FindbugsConfiguration(project, null, null, null));
    doReturn(plugin).when(conf).getFindbugsMavenPlugin();
    assertThat(conf.saveExcludeConfigXml(), is("rules/exclude.xml"));
    assertThat(conf.saveIncludeConfigXml(), is("rules/include.xml"));
  }
}
