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
import keyBy from 'lodash/keyBy';
import LoginSection from './LoginSection';
import LoginForm from './LoginForm';
import AboutProjects from './AboutProjects';
import AboutCleanCode from './AboutCleanCode';
import AboutIssues from './AboutIssues';
import AboutQualityGates from './AboutQualityGates';
import AboutLeakPeriod from './AboutLeakPeriod';
import AboutStandards from './AboutStandards';
import AboutScanners from './AboutScanners';
import { translate } from '../../../helpers/l10n';
import '../styles.css';
import { searchProjects } from '../../../api/components';
import { getFacet } from '../../../api/issues';

export default class AboutApp extends React.Component {
  state = {
    loading: true
  };

  componentDidMount () {
    this.mounted = true;
    this.loadData();
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  loadProjects () {
    return searchProjects({ ps: 1 }).then(r => r.paging.total);
  }

  loadIssues () {
    return getFacet({ resolved: false }, 'types').then(r => keyBy(r.facet, 'val'));
  }

  loadData () {
    Promise.all([
      window.sonarqube.appStarted,
      this.loadProjects(),
      this.loadIssues()
    ]).then(responses => {
      if (this.mounted) {
        const [options, projectsCount, issueTypes] = responses;
        this.setState({
          projectsCount,
          issueTypes,
          logoUrl: options.logoUrl,
          logoWidth: options.logoWidth,
          loading: false
        });
      }
    });
  }

  render () {
    if (this.state.loading) {
      return null;
    }

    const isAuthenticated = !!window.SS.user;
    const { signUpAllowed, landingText } = window.sonarqube;
    const loginFormShown = !isAuthenticated && this.props.location.query.login !== undefined;

    const logoUrl = this.state.logoUrl || `${window.baseUrl}/images/logo.svg`;
    const logoWidth = this.state.logoWidth || 100;
    const logoHeight = 30;
    const logoTitle = this.state.logoUrl ? '' : translate('layout.sonar.slogan');

    return (
        <div id="about-page" className="about-page">
          <div className="about-page-entry">

            <div className="about-page-logo">
              <img src={logoUrl} width={2 * logoWidth} height={2 * logoHeight} alt={logoTitle}/>
            </div>

            {loginFormShown ? (
                <div className="about-page-entry-box">
                  <LoginForm/>
                </div>
            ) : (
                <div className="about-page-entry-box">
                  <AboutProjects count={this.state.projectsCount}/>
                  {!isAuthenticated && <LoginSection/>}
                </div>
            )}

            {signUpAllowed && (
                <div className="about-page-sign-up">
                  No account yet? <a href={window.baseUrl + '/users/new'}>Sign up</a>
                </div>
            )}
          </div>

          {landingText.length > 0 && (
              <div className="about-page-section bordered-bottom">
                <div className="about-page-container" dangerouslySetInnerHTML={{ __html: landingText }}/>
              </div>
          )}

          <AboutCleanCode/>

          <AboutIssues
              bugs={this.state.issueTypes['BUG'].count}
              vulnerabilities={this.state.issueTypes['VULNERABILITY'].count}
              codeSmells={this.state.issueTypes['CODE_SMELL'].count}/>

          <AboutQualityGates/>

          <AboutLeakPeriod/>

          <AboutStandards/>

          <AboutScanners/>
        </div>
    );
  }
}
