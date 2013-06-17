package org.sonar.maven3;

import org.apache.maven.execution.MavenSession;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectBuilderContext;
import org.sonar.batch.scan.maven.MavenProjectConverter;

public class SonarMaven3ProjectBuilder extends ProjectBuilder {

  private MavenSession session;

  public SonarMaven3ProjectBuilder(MavenSession session) {
    this.session = session;
  }

  @Override
  public void build(ProjectBuilderContext context) {
    MavenProjectConverter.configure(context.getProjectReactor().getRoot(), session.getProjects(), session.getTopLevelProject());
  }

}
