package org.sonar.plugins.findbugs;

import org.sonar.api.BatchExtension;
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
    try {
      edu.umd.cs.findbugs.Project findbugsProject = new edu.umd.cs.findbugs.Project();
      for (File dir : project.getFileSystem().getSourceDirs()) {
        findbugsProject.addSourceDir(dir.getAbsolutePath());
      }
      findbugsProject.addFile(project.getFileSystem().getBuildOutputDir().getAbsolutePath());
      findbugsProject.setCurrentWorkingDirectory(project.getFileSystem().getBuildDir());
      return findbugsProject;
    } catch (Exception e) {
      throw new SonarException(e);
    }
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
}
