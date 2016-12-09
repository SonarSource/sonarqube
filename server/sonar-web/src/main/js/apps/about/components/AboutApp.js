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
import { connect } from 'react-redux';
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
import { searchProjects } from '../../../api/components';
import { getFacet } from '../../../api/issues';
import { getSettingValue } from '../../../store/rootReducer';
import * as settingsAPI from '../../../api/settings';
import '../styles.css';

class AboutApp extends React.Component {
  static propTypes = {
    customLogoUrl: React.PropTypes.string,
    customLogoWidth: React.PropTypes.oneOfType([React.PropTypes.string, React.PropTypes.number])
  };

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

  loadCustomText () {
    return settingsAPI.getSettingValue('sonar.lf.aboutText');
  }

  loadData () {
    Promise.all([
      this.loadProjects(),
      this.loadIssues(),
      this.loadCustomText()
    ]).then(responses => {
      if (this.mounted) {
        const [projectsCount, issues, customText] = responses;
        const issueTypes = keyBy(issues.facet, 'val');
        this.setState({
          projectsCount,
          issueTypes,
          customText,
          loading: false
        });
      }
    });
  }

  render () {
    if (this.state.loading) {
      return null;
    }

    const { customText } = this.state;

    const logoUrl = this.props.customLogoUrl || `${window.baseUrl}/images/logo.svg`;
    const logoWidth = Number(this.props.customLogoWidth || 100);
    const logoHeight = 30;
    const logoTitle = this.props.customLogoUrl ? '' : translate('layout.sonar.slogan');

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

            {customText != null && customText.length > 0 && (
                <div className="about-page-section" dangerouslySetInnerHTML={{ __html: customText }}/>
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

const mapStateToProps = state => ({
  customLogoUrl: (getSettingValue(state, 'sonar.lf.logoUrl') || {}).value,
  customLogoWidth: (getSettingValue(state, 'sonar.lf.logoWidthPx') || {}).value
});

export default connect(mapStateToProps)(AboutApp);
