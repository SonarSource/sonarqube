/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import { Link } from 'react-router';
import AboutProjects from './AboutProjects';
import EntryIssueTypesForSonarQubeDotCom from './EntryIssueTypesForSonarQubeDotCom';
import AboutRulesForSonarQubeDotCom from './AboutRulesForSonarQubeDotCom';
import AboutCleanCode from './AboutCleanCode';
import AboutQualityModelForSonarQubeDotCom from './AboutQualityModelForSonarQubeDotCom';
import AboutQualityGates from './AboutQualityGates';
import AboutLeakPeriod from './AboutLeakPeriod';
import AboutStandards from './AboutStandards';
import AboutScanners from './AboutScanners';
import { translate } from '../../../helpers/l10n';
import '../sonarqube-dot-com-styles.css';

export default class AboutAppForSonarQubeDotCom extends React.Component {
  props: {
    bugs: number,
    codeSmells: number,
    currentUser: { isLoggedIn: boolean },
    customText?: string,
    projectsCount: number,
    vulnerabilities: number
  };

  render() {
    const { customText } = this.props;

    return (
      <div id="about-page" className="about-page sqcom-about-page">
        <div className="sqcom-about-page-entry">
          <div className="about-page-container">
            <div className="sqcom-about-page-intro">
              <h1 className="big-spacer-bottom">
                Continuous Code Quality<br />as a Service
              </h1>
              <a
                className="button button-active"
                href="https://about.sonarqube.com/get-started/"
                target="_blank">
                Get Started
              </a>
              {!this.props.currentUser.isLoggedIn &&
                <Link to="/sessions/new" className="button big-spacer-left">
                  {translate('layout.login')}
                </Link>}
            </div>

            <div className="sqcom-about-page-instance">
              <AboutProjects count={this.props.projectsCount} />
              <EntryIssueTypesForSonarQubeDotCom
                bugs={this.props.bugs}
                vulnerabilities={this.props.vulnerabilities}
                codeSmells={this.props.codeSmells}
              />
            </div>
          </div>
        </div>

        <AboutRulesForSonarQubeDotCom />

        <div className="about-page-container">
          {customText != null &&
            customText.value &&
            <div
              className="about-page-section"
              dangerouslySetInnerHTML={{ __html: customText.value }}
            />}

          <AboutQualityModelForSonarQubeDotCom />

          <div className="flex-columns">
            <div className="flex-column flex-column-half about-page-group-boxes">
              <AboutCleanCode />
            </div>
            <div className="flex-column flex-column-half about-page-group-boxes">
              <AboutLeakPeriod />
            </div>
          </div>

          <div className="flex-columns">
            <div className="flex-column flex-column-half about-page-group-boxes">
              <AboutQualityGates />
            </div>
            <div className="flex-column flex-column-half about-page-group-boxes">
              <AboutStandards />
            </div>
          </div>

          <AboutScanners />
        </div>
      </div>
    );
  }
}
