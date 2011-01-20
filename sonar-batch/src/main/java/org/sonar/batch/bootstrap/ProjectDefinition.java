package org.sonar.batch.bootstrap;

import com.google.common.collect.Lists;
import org.apache.commons.configuration.Configuration;
import org.sonar.api.resources.ProjectDirectory;

import java.io.File;
import java.util.List;

/**
 * Defines project in a form suitable to bootstrap Sonar batch.
 * We assume that project is just a set of configuration properties and directories.
 * This is a part of bootstrap process, so we should take care about backward compatibility.
 * 
 * @since 2.6
 */
public class ProjectDefinition {

  private Configuration configuration;

  private File workDir;
  private File basedir;
  private List<ProjectDirectory> dirs = Lists.newArrayList();

  private ProjectDefinition parent;
  private List<ProjectDefinition> modules;

  /**
   * @return project properties.
   */
  public Configuration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * @return Sonar working directory for this project.
   *         It's "${project.build.directory}/sonar" ("${project.basedir}/target/sonar") for Maven projects.
   */
  public File getSonarWorkingDirectory() {
    return workDir;
  }

  public void setSonarWorkingDirectory(File workDir) {
    this.workDir = workDir;
  }

  /**
   * @return project root directory.
   *         It's "${project.basedir}" for Maven projects.
   */
  public File getBasedir() {
    return basedir;
  }

  public void setBasedir(File basedir) {
    this.basedir = basedir;
  }

  /**
   * @return project directories.
   */
  public List<ProjectDirectory> getDirs() {
    return dirs;
  }

  public void addDir(ProjectDirectory dir) {
    this.dirs.add(dir);
  }

  /**
   * @return parent project.
   */
  public ProjectDefinition getParent() {
    return parent;
  }

  public void setParent(ProjectDefinition parent) {
    this.parent = parent;
    if (parent != null) {
      parent.modules.add(this);
    }
  }

  /**
   * @return list of sub-projects.
   */
  public List<ProjectDefinition> getModules() {
    return modules;
  }

}
