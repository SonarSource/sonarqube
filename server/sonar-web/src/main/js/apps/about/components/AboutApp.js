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
import AboutProjects from './AboutProjects';
import EntryIssueTypes from './EntryIssueTypes';
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
    document.querySelector('html').classList.add('dashboard-page');
    this.loadData();
  }

  componentWillUnmount () {
    document.querySelector('html').classList.remove('dashboard-page');
    this.mounted = false;
  }

  loadProjects () {
    return searchProjects({ ps: 1 }).then(r => r.paging.total);
  }

  loadIssues () {
    return getFacet({ resolved: false }, 'types');
  }

  loadData () {
    Promise.all([
      this.loadProjects(),
      this.loadIssues()
    ]).then(responses => {
      if (this.mounted) {
        // FIXME
        const options = {};

        const [projectsCount, issues] = responses;
        const issueTypes = keyBy(issues.facet, 'val');
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

    // FIXME
    const landingText = '';

    const logoUrl = this.state.logoUrl || `${window.baseUrl}/images/logo.svg`;
    const logoWidth = this.state.logoWidth || 100;
    const logoHeight = 30;
    const logoTitle = this.state.logoUrl ? '' : translate('layout.sonar.slogan');

    return (
        <div id="about-page" className="about-page">
          <div className="about-page-entry">
            <div className="about-page-container clearfix">
              <div className="pull-left">
                <div className="about-page-logo">
                  <img src={logoUrl} width={2 * logoWidth} height={2 * logoHeight} alt={logoTitle}/>
                </div>
              </div>

              <div className="about-page-entry-column">
                <EntryIssueTypes
                    bugs={this.state.issueTypes['BUG'].count}
                    vulnerabilities={this.state.issueTypes['VULNERABILITY'].count}
                    codeSmells={this.state.issueTypes['CODE_SMELL'].count}/>
              </div>

              <div className="about-page-entry-column">
                <AboutProjects count={this.state.projectsCount}/>
              </div>
            </div>
          </div>

          <div className="about-page-container">

            {landingText.length > 0 && (
                <div className="about-page-section" dangerouslySetInnerHTML={{ __html: landingText }}/>
            )}

            <div className="columns">
              <div className="column-two-thirds">
                <AboutCleanCode/>
                <AboutLeakPeriod/>
              </div>
              <div className="column-third">
                <AboutIssues/>
              </div>
            </div>

            <div className="columns">
              <div className="column-half">
                <AboutQualityGates/>
              </div>
              <div className="column-half">
                <AboutStandards/>
              </div>
            </div>

            <AboutScanners/>
          </div>
        </div>
    );
  }
}
