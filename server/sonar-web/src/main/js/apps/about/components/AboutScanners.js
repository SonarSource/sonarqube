/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';

const links = {
  sonarqube: 'http://redirect.sonarsource.com/doc/install-configure-scanner.html',
  msbuild: 'http://redirect.sonarsource.com/doc/install-configure-scanner-msbuild.html',
  maven: 'http://redirect.sonarsource.com/doc/install-configure-scanner-maven.html',
  gradle: 'http://redirect.sonarsource.com/doc/gradle.html',
  jenkins: 'http://redirect.sonarsource.com/plugins/jenkins.html',
  ant: 'http://redirect.sonarsource.com/doc/install-configure-scanner-ant.html'
};

export default class AboutScanners extends React.Component {
  render () {
    return (
        <div className="about-page-section">
          <div className="about-page-container">
            <h2 className="about-page-header text-center">Start analyzing your projects with a SonarQube Scanner</h2>
            <div className="about-page-analyzers">
              <div className="about-page-analyzer-box">
                <div className=" big-spacer-bottom">
                  <img src={window.baseUrl + '/images/scanner-logos/sonarqube.svg'} height={80}
                       alt="SonarQube Scanner"/>
                </div>
                <p className="about-page-text">
                  This Java-based command-line tool can analyze any languages SonarQube supports.
                </p>
                <div className="big-spacer-top">
                  <a className="about-page-link-more" href={links.sonarqube} target="_blank">
                    <span>Read more</span>
                    <i className="icon-detach spacer-left"/>
                  </a>
                </div>
              </div>
              <div className="about-page-analyzer-box">
                <div className=" big-spacer-bottom">
                  <img src={window.baseUrl + '/images/scanner-logos/msbuild.svg'} height={80}
                       alt="SonarQube Scanner for MSBuild"/>
                </div>
                <p className="about-page-text">
                  Built in collaboration with Microsoft this is the recommended way to launch a SonarQube analysis on
                  MSBuild projects and solutions.
                </p>
                <div className="big-spacer-top">
                  <a className="about-page-link-more" href={links.msbuild} target="_blank">
                    <span>Read more</span>
                    <i className="icon-detach spacer-left"/>
                  </a>
                </div>
              </div>
              <div className="about-page-analyzer-box">
                <div className=" big-spacer-bottom">
                  <img src={window.baseUrl + '/images/scanner-logos/maven.svg'} height={80}
                       alt="SonarQube Scanner for Maven"/>
                </div>
                <p className="about-page-text">
                  Using the SonarQube Scanner for Maven is as simple as running <code>mvn sonar:sonar</code> on your
                  Maven project.
                </p>
                <div className="big-spacer-top">
                  <a className="about-page-link-more" href={links.maven} target="_blank">
                    <span>Read more</span>
                    <i className="icon-detach spacer-left"/>
                  </a>
                </div>
              </div>
              <div className="about-page-analyzer-box">
                <div className=" big-spacer-bottom">
                  <img src={window.baseUrl + '/images/scanner-logos/gradle.svg'} height={80}
                       alt="SonarQube Scanner for Gradle"/>
                </div>
                <p className="about-page-text">
                  The SonarQube Scanner for Gradle provides an easy way to start analysis of a Gradle project.
                </p>
                <div className="big-spacer-top">
                  <a className="about-page-link-more" href={links.gradle} target="_blank">
                    <span>Read more</span>
                    <i className="icon-detach spacer-left"/>
                  </a>
                </div>
              </div>
              <div className="about-page-analyzer-box">
                <div className=" big-spacer-bottom">
                  <img src={window.baseUrl + '/images/scanner-logos/jenkins.svg'} height={80}
                       alt="SonarQube Scanner for Jenkins"/>
                </div>
                <p className="about-page-text">
                  The SonarQube Scanner for Jenkins lets you integrate analysis seamlessly into a job or a pipeline.
                </p>
                <div className="big-spacer-top">
                  <a className="about-page-link-more" href={links.jenkins} target="_blank">
                    <span>Read more</span>
                    <i className="icon-detach spacer-left"/>
                  </a>
                </div>
              </div>
              <div className="about-page-analyzer-box">
                <div className=" big-spacer-bottom">
                  <img src={window.baseUrl + '/images/scanner-logos/ant.svg'} height={80}
                       alt="SonarQube Scanner for Ant"/>
                </div>
                <p className="about-page-text">
                  The SonarQube Scanner for Ant lets you start an analysis directly from an Apache Ant script.
                </p>
                <div className="big-spacer-top">
                  <a className="about-page-link-more" href={links.ant} target="_blank">
                    <span>Read more</span>
                    <i className="icon-detach spacer-left"/>
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>
    );
  }
}
