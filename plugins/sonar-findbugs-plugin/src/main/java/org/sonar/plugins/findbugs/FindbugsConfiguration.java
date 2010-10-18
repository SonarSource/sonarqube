package org.sonar.plugins.findbugs;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.findbugs.xml.ClassFilter;
import org.sonar.plugins.findbugs.xml.FindBugsFilter;
import org.sonar.plugins.findbugs.xml.Match;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

/**
 * @since 2.4
 */
public class FindbugsConfiguration implements BatchExtension {

  private Project project;
  private RulesProfile profile;
  private FindbugsProfileExporter exporter;

  public FindbugsConfiguration(Project project, RulesProfile profile, FindbugsProfileExporter exporter) {
    this.project = project;
    this.profile = profile;
    this.exporter = exporter;
  }

  public File getTargetXMLReport() {
    if (project.getConfiguration().getBoolean(FindbugsConstants.GENERATE_XML_KEY, FindbugsConstants.GENERATE_XML_DEFAULT_VALUE)) {
      return new File(project.getFileSystem().getSonarWorkingDirectory(), "findbugs-result.xml");
    }
    return null;
  }

  public edu.umd.cs.findbugs.Project getFindbugsProject() {
    File classesDir = project.getFileSystem().getBuildOutputDir();
    if (classesDir == null || !classesDir.exists()) {
      throw new SonarException("Findbugs needs sources to be compiled. "
          + "Please build project or edit pom.xml to set the <outputDirectory> property before executing sonar.");
    }

    edu.umd.cs.findbugs.Project findbugsProject = new edu.umd.cs.findbugs.Project();
    for (File dir : project.getFileSystem().getSourceDirs()) {
      findbugsProject.addSourceDir(dir.getAbsolutePath());
    }
    findbugsProject.addFile(classesDir.getAbsolutePath());
    findbugsProject.setCurrentWorkingDirectory(project.getFileSystem().getBuildDir());
    return findbugsProject;
  }

  public File saveIncludeConfigXml() throws IOException {
    StringWriter conf = new StringWriter();
    exporter.exportProfile(profile, conf);
    return project.getFileSystem().writeToWorkingDirectory(conf.toString(), "findbugs-include.xml");
  }

  public File saveExcludeConfigXml() throws IOException {
    FindBugsFilter findBugsFilter = new FindBugsFilter();
    if (project.getExclusionPatterns() != null) {
      for (String exclusion : project.getExclusionPatterns()) {
        ClassFilter classFilter = new ClassFilter(FindbugsAntConverter.antToJavaRegexpConvertor(exclusion));
        findBugsFilter.addMatch(new Match(classFilter));
      }
    }
    return project.getFileSystem().writeToWorkingDirectory(findBugsFilter.toXml(), "findbugs-exclude.xml");
  }

  public String getEffort() {
    return StringUtils.lowerCase(project.getConfiguration().getString(CoreProperties.FINDBUGS_EFFORT_PROPERTY, CoreProperties.FINDBUGS_EFFORT_DEFAULT_VALUE));
  }

  public long getTimeout() {
    return project.getConfiguration().getLong(CoreProperties.FINDBUGS_TIMEOUT_PROPERTY, FindbugsConstants.FINDBUGS_TIMEOUT_DEFAULT_VALUE);
  }
}
