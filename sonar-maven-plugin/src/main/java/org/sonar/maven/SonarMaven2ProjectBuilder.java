package org.sonar.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectBuilderContext;
import org.sonar.batch.scan.maven.MavenProjectConverter;

import java.util.List;

public class SonarMaven2ProjectBuilder extends ProjectBuilder {

  private MavenSession session;

  public SonarMaven2ProjectBuilder(MavenSession session) {
    this.session = session;
  }

  @Override
  public void build(ProjectBuilderContext context) {
    List<MavenProject> sortedProjects = session.getSortedProjects();
    MavenProject topLevelProject = null;
    for (MavenProject project : sortedProjects) {
      if (project.isExecutionRoot()) {
        topLevelProject = project;
        break;
      }
    }
    MavenProjectConverter.configure(context.getProjectReactor().getRoot(), sortedProjects, topLevelProject);
  }

}
