/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.updatecenter.mavenplugin;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.License;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.traversal.BuildingDependencyNodeVisitor;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.sonar.updatecenter.common.FormatUtils;
import org.sonar.updatecenter.common.PluginManifest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Build a Sonar Plugin from the current project.
 * 
 * @author Evgeny Mandrikov
 * @goal sonar-plugin
 * @phase package
 * @requiresProject
 * @requiresDependencyResolution runtime
 * @threadSafe
 */
public class SonarPluginMojo extends AbstractSonarPluginMojo {
  private static final String LIB_DIR = "META-INF/lib/";
  private static final String[] DEFAULT_EXCLUDES = new String[] { "**/package.html" };
  private static final String[] DEFAULT_INCLUDES = new String[] { "**/**" };

  /**
   * List of files to include. Specified as fileset patterns which are relative to the input directory whose contents
   * is being packaged into the JAR.
   * 
   * @parameter
   */
  private String[] includes;

  /**
   * List of files to exclude. Specified as fileset patterns which are relative to the input directory whose contents
   * is being packaged into the JAR.
   * 
   * @parameter
   */
  private String[] excludes;

  /**
   * The Jar archiver.
   * 
   * @component role="org.codehaus.plexus.archiver.Archiver" role-hint="jar"
   */
  protected JarArchiver jarArchiver;

  /**
   * The archive configuration to use.
   * See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver Reference</a>.
   * 
   * @parameter
   */
  private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

  /**
   * @component
   * @required
   * @readonly
   */
  private DependencyTreeBuilder dependencyTreeBuilder;

  /**
   * The artifact repository to use.
   * 
   * @parameter expression="${localRepository}"
   * @required
   * @readonly
   */
  private ArtifactRepository localRepository;

  /**
   * The artifact factory to use.
   * 
   * @component
   * @required
   * @readonly
   */
  private ArtifactFactory artifactFactory;

  /**
   * The artifact metadata source to use.
   * 
   * @component
   * @required
   * @readonly
   */
  private ArtifactMetadataSource artifactMetadataSource;

  /**
   * The artifact collector to use.
   * 
   * @component
   * @required
   * @readonly
   */
  private ArtifactCollector artifactCollector;

  /**
   * @parameter expression="${sonar.addMavenDescriptor}"
   */
  private boolean addMavenDescriptor = true;

  public void execute() throws MojoExecutionException, MojoFailureException {
    File jarFile = createArchive();
    String classifier = getClassifier();
    if (classifier != null) {
      projectHelper.attachArtifact(getProject(), "jar", classifier, jarFile);
    } else {
      getProject().getArtifact().setFile(jarFile);
    }
  }

  public File createArchive() throws MojoExecutionException {
    checkPluginClass();

    File jarFile = getJarFile(getOutputDirectory(), getFinalName(), getClassifier());
    MavenArchiver archiver = new MavenArchiver();
    archiver.setArchiver(jarArchiver);
    archiver.setOutputFile(jarFile);

    try {
      archiver.getArchiver().addDirectory(getClassesDirectory(), getIncludes(), getExcludes());
      archive.setAddMavenDescriptor(addMavenDescriptor);
      getLog().info("-------------------------------------------------------");
      getLog().info("Plugin definition in update center");
      addManifestProperty("Key", PluginManifest.KEY, getPluginKey());
      addManifestProperty("Name", PluginManifest.NAME, getPluginName());
      addManifestProperty("Description", PluginManifest.DESCRIPTION, getPluginDescription());
      addManifestProperty("Version", PluginManifest.VERSION, getProject().getVersion());
      addManifestProperty("Main class", PluginManifest.MAIN_CLASS, getPluginClass());
      addManifestProperty("Homepage", PluginManifest.HOMEPAGE, getPluginUrl());
      addManifestProperty("Sonar version", PluginManifest.SONAR_VERSION, getSonarPluginApiArtifact().getVersion());
      addManifestProperty("License", PluginManifest.LICENSE, getPluginLicense());
      addManifestProperty("Organization", PluginManifest.ORGANIZATION, getPluginOrganization());
      addManifestProperty("Organization URL", PluginManifest.ORGANIZATION_URL, getPluginOrganizationUrl());
      addManifestProperty("Terms & Conditions URL", PluginManifest.TERMS_CONDITIONS_URL, getPluginTermsConditionsUrl());
      addManifestProperty("Issue Tracker URL", PluginManifest.ISSUE_TRACKER_URL, getPluginIssueTrackerUrl());
      addManifestProperty("Build date", PluginManifest.BUILD_DATE, FormatUtils.toString(new Date(), true));
      getLog().info("-------------------------------------------------------");

      if (isUseChildFirstClassLoader()) {
        archive.addManifestEntry(PluginManifest.USE_CHILD_FIRST_CLASSLOADER, true);
      }

      if (isSkipDependenciesPackaging()) {
        getLog().info("Skip packaging of dependencies");

      } else {
        List<String> libs = copyDependencies();
        if ( !libs.isEmpty()) {
          archiver.getArchiver().addDirectory(getAppDirectory(), getIncludes(), getExcludes());
          archive.addManifestEntry(PluginManifest.DEPENDENCIES, StringUtils.join(libs, " "));
        }
      }

      archiver.createArchive(getProject(), archive);
      return jarFile;

    } catch (Exception e) {
      throw new MojoExecutionException("Error assembling Sonar-plugin: " + e.getMessage(), e);
    }
  }

  private void addManifestProperty(String label, String key, String value) {
    getLog().info("    " + label + ": " + StringUtils.defaultString(value));
    archive.addManifestEntry(key, value);
  }

  private String getPluginLicense() {
    List<String> licenses = new ArrayList<String>();
    if (getProject().getLicenses() != null) {
      for (Object license : getProject().getLicenses()) {
        License l = (License) license;
        if (l.getName() != null) {
          licenses.add(l.getName());
        }
      }
    }
    return StringUtils.join(licenses, " ");
  }

  private String getPluginOrganization() {
    if (getProject().getOrganization() != null) {
      return getProject().getOrganization().getName();
    }
    return null;
  }

  private String getPluginOrganizationUrl() {
    if (getProject().getOrganization() != null) {
      return getProject().getOrganization().getUrl();
    }
    return null;
  }

  private void checkPluginClass() throws MojoExecutionException {
    if ( !new File(getClassesDirectory(), getPluginClass().replace('.', '/') + ".class").exists()) {
      throw new MojoExecutionException("Error assembling Sonar-plugin: Plugin-Class '" + getPluginClass() + "' not found");
    }
  }

  protected static File getJarFile(File basedir, String finalName, String classifier) {
    if (classifier == null) {
      classifier = "";
    } else if (classifier.trim().length() > 0 && !classifier.startsWith("-")) {
      classifier = "-" + classifier;
    }
    return new File(basedir, finalName + classifier + ".jar");
  }

  private List<String> copyDependencies() throws IOException, DependencyTreeBuilderException {
    List<String> ids = new ArrayList<String>();
    List<String> libs = new ArrayList<String>();
    File libDirectory = new File(getAppDirectory(), LIB_DIR);
    Set<Artifact> artifacts = getNotProvidedDependencies();
    for (Artifact artifact : artifacts) {
      String targetFileName = getDefaultFinalName(artifact);
      FileUtils.copyFileIfModified(artifact.getFile(), new File(libDirectory, targetFileName));
      libs.add(LIB_DIR + targetFileName);
      ids.add(artifact.getDependencyConflictId());
    }

    if ( !ids.isEmpty()) {
      getLog().info(getMessage("Following dependencies are packaged in the plugin:", ids));
      getLog().info(new StringBuilder()
          .append("See following page for more details about plugin dependencies:\n")
          .append("\n\thttp://docs.codehaus.org/display/SONAR/Coding+a+plugin\n")
          .toString()
          );
    }
    return libs;
  }

  private String getDefaultFinalName(Artifact artifact) {
    return artifact.getArtifactId() + "-" + artifact.getVersion() + "." + artifact.getArtifactHandler().getExtension();
  }

  private Set<Artifact> getNotProvidedDependencies() throws DependencyTreeBuilderException {
    Set<Artifact> result = new HashSet<Artifact>();
    Set<Artifact> providedArtifacts = getSonarProvidedArtifacts();
    for (Artifact artifact : getIncludedArtifacts()) {
      if ("sonar-plugin".equals(artifact.getType())) {
        continue;
      }
      if (Artifact.SCOPE_PROVIDED.equals(artifact.getScope()) || Artifact.SCOPE_TEST.equals(artifact.getScope())) {
        continue;
      }
      if (containsArtifact(providedArtifacts, artifact)) {
        continue;
      }
      result.add(artifact);
    }
    return result;
  }

  private boolean containsArtifact(Set<Artifact> artifacts, Artifact artifact) {
    for (Artifact a : artifacts) {
      if (StringUtils.equals(a.getGroupId(), artifact.getGroupId()) &&
          StringUtils.equals(a.getArtifactId(), artifact.getArtifactId())) {
        return true;
      }
    }
    return false;
  }

  private Set<Artifact> getSonarProvidedArtifacts() throws DependencyTreeBuilderException {
    Set<Artifact> result = new HashSet<Artifact>();
    ArtifactFilter artifactFilter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);
    DependencyNode rootNode = dependencyTreeBuilder.buildDependencyTree(getProject(), localRepository, artifactFactory,
        artifactMetadataSource, artifactFilter, artifactCollector);
    rootNode.accept(new BuildingDependencyNodeVisitor());
    searchForSonarProvidedArtifacts(rootNode, result, false);
    return result;
  }

  private void searchForSonarProvidedArtifacts(DependencyNode dependency, Set<Artifact> sonarArtifacts, boolean isProvidedBySonar) {
    if (dependency != null) {
      // skip check on root node - see SONAR-1815
      if (dependency.getParent() != null) {
        isProvidedBySonar = isProvidedBySonar || ("org.codehaus.sonar".equals(dependency.getArtifact().getGroupId()) && !Artifact.SCOPE_TEST.equals(dependency.getArtifact().getScope()));
      }

      if (isProvidedBySonar) {
        sonarArtifacts.add(dependency.getArtifact());
      }

      if ( !Artifact.SCOPE_TEST.equals(dependency.getArtifact().getScope())) {
        for (Object childDep : dependency.getChildren()) {
          searchForSonarProvidedArtifacts((DependencyNode) childDep, sonarArtifacts, isProvidedBySonar);
        }
      }
    }
  }

  private String[] getIncludes() {
    if (includes != null && includes.length > 0) {
      return includes;
    }
    return DEFAULT_INCLUDES;
  }

  private String[] getExcludes() {
    if (excludes != null && excludes.length > 0) {
      return excludes;
    }
    return DEFAULT_EXCLUDES;
  }

}
