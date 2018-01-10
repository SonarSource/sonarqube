/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package util;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.junit.rules.ExternalResource;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;
import static util.ItUtils.resetSettings;

/**
 * Rule wrapping around an {@link Orchestrator} instance which handle:
 * <ul>
 *   <li>automatic reset of Orchestrator data after each method when used as a {@link org.junit.Rule}, 
 *      after each class when used as a {@link org.junit.ClassRule}</li>
 *   <li>automatic reset of server properties after each method when used as a {@link org.junit.Rule}, 
 *      after each class when used as a {@link org.junit.ClassRule}</li>
 *   <li>associating project with a specific Quality Profile before running an analysis</li>
 *   <li>provisioning a project before its first analysis so that a Quality Profile can be associated to it</li>
 *   <li>"restoring" a Quality Profile before an analysis with a specific Quality Profile</li>
 * </ul>
 *
 * This Rule has preparatory methods ({@link #registerProfile(String)} and {@link #registerProject(String)}) which
 * will allow consequent calls to the rule methods to be based solely on Quality Profile and Project keys. In addition,
 * these methods returns the Quality Profile and Project key to avoid information duplication (the only magic string
 * the IT developer has to know is the relative path of the Project or the Quality Profile).
 *
 * To run an analysis, use method {@link #newProjectAnalysis(String)} to create a {@link ProjectAnalysis}
 * object. This object has a {@link ProjectAnalysis#run()} method which will start the analysis.
 * {@link ProjectAnalysis} can safely be reused to run the same analysis multiple times. In addition, these objects are
 * immutable. Any call to one of their method which would modify their state will create a new instance which can also
 * be reused at will.
 */
public class ProjectAnalysisRule extends ExternalResource {

  private final Orchestrator orchestrator;
  private final LoadedProfiles loadedProfiles = new LoadedProfiles();
  private final LoadedProjects loadedProjects = new LoadedProjects();
  private final Set<String> serverProperties = new HashSet<>();

  private ProjectAnalysisRule(Orchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  public static ProjectAnalysisRule from(Orchestrator orchestrator) {
    return new ProjectAnalysisRule(requireNonNull(orchestrator, "Orchestrator instance can not be null"));
  }

  /**
   * @param relativePathToProfile eg.: "/issue/suite/IssueFilterExtensionTest/xoo-with-many-rules.xml"
   *
   * @return the quality profile key
   */
  public String registerProfile(String relativePathToProfile) {
    return this.loadedProfiles.loadProfile(relativePathToProfile);
  }

  /**
   * @param projectRelativePath path relative to it/projects, eg. "shared/xoo-multi-modules-sample"
   *
   * @return the project key
   */
  public String registerProject(String projectRelativePath) {
    return this.loadedProjects.load(projectRelativePath);
  }

  public ProjectAnalysis newProjectAnalysis(String projectKey) {
    ProjectState projectState = this.loadedProjects.getProjectState(projectKey);

    return new ProjectAnalysisImpl(projectState, null, false);
  }

  @Override
  protected void before() {
    orchestrator.resetData();
  }

  @Override
  protected void after() {
    resetServerProperties();
    resetRuleState();
  }

  private void resetServerProperties() {
    resetSettings(orchestrator, null, serverProperties.toArray(new String[] {}));
  }

  public void setServerPropertyImpl(String key, @Nullable String value) {
    ItUtils.setServerProperty(orchestrator, key, value);
  }

  public ProjectAnalysisRule setServerProperty(String key, String value) {
    setServerPropertyImpl(key, value);
    this.serverProperties.add(key);
    return this;
  }

  @Immutable
  private final class ProjectAnalysisImpl implements ProjectAnalysis {
    private final ProjectState projectState;
    @CheckForNull
    private final Profile qualityProfile;
    private final boolean debugLogs;
    @CheckForNull
    private final String[] properties;

    private ProjectAnalysisImpl(ProjectState projectState, @Nullable Profile qualityProfile, boolean debugLogs, String... properties) {
      this.projectState = projectState;
      this.qualityProfile = qualityProfile;
      this.debugLogs = debugLogs;
      this.properties = properties;
    }

    @Override
    public ProjectAnalysis withQualityProfile(String qualityProfileKey) {
      checkNotNull(qualityProfileKey, "Specified Quality Profile Key can not be null");
      if (this.qualityProfile != null && this.qualityProfile.getProfileKey().equals(qualityProfileKey)) {
        return this;
      }

      return new ProjectAnalysisImpl(this.projectState, loadedProfiles.getState(qualityProfileKey), this.debugLogs, this.properties);
    }

    @Override
    public ProjectAnalysis withXooEmptyProfile() {
      if (this.qualityProfile == Profile.XOO_EMPTY_PROFILE) {
        return this;
      }
      return new ProjectAnalysisImpl(this.projectState, Profile.XOO_EMPTY_PROFILE, this.debugLogs, this.properties);
    }

    @Override
    public ProjectAnalysis withDebugLogs(boolean enabled) {
      if (this.debugLogs == enabled) {
        return this;
      }
      return new ProjectAnalysisImpl(this.projectState, this.qualityProfile, enabled, this.properties);
    }

    @Override
    public ProjectAnalysis withProperties(String... properties) {
      checkArgument(
        properties == null || properties.length % 2 == 0,
        "there must be an even number of String parameters (got %s): key/value pairs must be complete", properties == null ? 0 : properties.length);
      return new ProjectAnalysisImpl(this.projectState, this.qualityProfile, this.debugLogs, properties);
    }

    @Override
    public void run() {
      provisionIfNecessary();
      setQualityProfileIfNecessary();
      runAnalysis();
    }

    private void setQualityProfileIfNecessary() {
      if (this.qualityProfile != null) {
        if (this.qualityProfile != Profile.XOO_EMPTY_PROFILE) {
          ItUtils.restoreProfile(orchestrator, getClass().getResource(this.qualityProfile.getRelativePath()));
        }
        orchestrator.getServer().associateProjectToQualityProfile(
          this.projectState.getProjectKey(),
          this.qualityProfile.getLanguageKey(),
          this.qualityProfile.getProfileKey());
      }
    }

    private void provisionIfNecessary() {
      if (this.qualityProfile != null && !projectState.isProvisioned()) {
        String projectKey = projectState.getProjectKey();
        orchestrator.getServer().provisionProject(projectKey, MoreObjects.firstNonNull(projectState.getProjectName(), projectKey));
        projectState.setProvisioned(true);
      }
    }

    private SonarRunner runAnalysis() {
      SonarRunner sonarRunner = SonarRunner.create(projectState.getProjectDir());
      ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
      for (int i = 0; i < this.properties.length; i += 2) {
        builder.put(this.properties[i], this.properties[i + 1]);
      }
      SonarRunner scan = sonarRunner.setDebugLogs(this.debugLogs).setProperties(builder.build());
      orchestrator.executeBuild(scan);
      return scan;
    }
  }

  private void resetRuleState() {
    this.loadedProjects.reset();
    this.loadedProfiles.reset();
    this.serverProperties.clear();
  }

}
