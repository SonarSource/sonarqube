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
import * as React from 'react';
import { connect } from 'react-redux';
import { keyBy } from 'lodash';
import { Link } from 'react-router';
import { Location } from 'history';
import AboutProjects from './AboutProjects';
import EntryIssueTypes from './EntryIssueTypes';
import AboutLanguages from './AboutLanguages';
import AboutCleanCode from './AboutCleanCode';
import AboutQualityModel from './AboutQualityModel';
import AboutQualityGates from './AboutQualityGates';
import AboutLeakPeriod from './AboutLeakPeriod';
import AboutStandards from './AboutStandards';
import AboutScanners from './AboutScanners';
import { searchProjects } from '../../../api/components';
import { getFacet } from '../../../api/issues';
import GlobalContainer from '../../../app/components/GlobalContainer';
import {
  getAppState,
  getCurrentUser,
  getGlobalSettingValue,
  Store
} from '../../../store/rootReducer';
import { translate } from '../../../helpers/l10n';
import { fetchAboutPageSettings } from '../actions';
import { IssueType, AppState, CurrentUser } from '../../../app/types';
import '../styles.css';

interface Props {
  appState: Pick<AppState, 'defaultOrganization' | 'organizationsEnabled'>;
  currentUser: CurrentUser;
  customText?: string;
  fetchAboutPageSettings: () => Promise<void>;
  location: Location;
}

interface State {
  issueTypes?: { [key: string]: { count: number } };
  loading: boolean;
  projectsCount: number;
}

class AboutApp extends React.PureComponent<Props, State> {
  mounted = false;

  state: State = {
    loading: true,
    projectsCount: 0
  };

  componentDidMount() {
    this.mounted = true;
    this.loadData();
    document.body.classList.add('white-page');
    if (document.documentElement) {
      document.documentElement.classList.add('white-page');
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    document.body.classList.remove('white-page');
    if (document.documentElement) {
      document.documentElement.classList.remove('white-page');
    }
  }

  loadProjects() {
    return searchProjects({ ps: 1 }).then(r => r.paging.total);
  }

  loadIssues() {
    return getFacet({ resolved: false }, 'types');
  }

  loadCustomText() {
    return this.props.fetchAboutPageSettings();
  }

  loadData() {
    Promise.all([this.loadProjects(), this.loadIssues(), this.loadCustomText()]).then(
      responses => {
        if (this.mounted) {
          const [projectsCount, issues] = responses;
          const issueTypes = keyBy(issues.facet, 'val');
          this.setState({ projectsCount, issueTypes, loading: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  }

  render() {
    const { customText } = this.props;
    const { loading, issueTypes, projectsCount } = this.state;

    let bugs;
    let vulnerabilities;
    let codeSmells;
    if (!loading && issueTypes) {
      bugs = issueTypes[IssueType.Bug] && issueTypes[IssueType.Bug].count;
      vulnerabilities =
        issueTypes[IssueType.Vulnerability] && issueTypes[IssueType.Vulnerability].count;
      codeSmells = issueTypes[IssueType.CodeSmell] && issueTypes[IssueType.CodeSmell].count;
    }

    return (
      <GlobalContainer location={this.props.location}>
        <div className="page page-limited about-page" id="about-page">
          <div className="about-page-entry">
            <div className="about-page-intro">
              <h1 className="big-spacer-bottom">{translate('layout.sonar.slogan')}</h1>
              {!this.props.currentUser.isLoggedIn && (
                <Link className="button button-active big-spacer-right" to="/sessions/new">
                  {translate('layout.login')}
                </Link>
              )}
              <a
                className="button"
                href="https://redirect.sonarsource.com/doc/home.html"
                rel="noopener noreferrer"
                target="_blank">
                {translate('about_page.read_documentation')}
              </a>
            </div>

            <div className="about-page-instance">
              <AboutProjects count={projectsCount} loading={loading} />
              <EntryIssueTypes
                bugs={bugs}
                codeSmells={codeSmells}
                loading={loading}
                vulnerabilities={vulnerabilities}
              />
            </div>
          </div>

          {customText && (
            <div className="about-page-section" dangerouslySetInnerHTML={{ __html: customText }} />
          )}

          <AboutLanguages />

          <AboutQualityModel />

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
              <AboutStandards appState={this.props.appState} />
            </div>
          </div>

          <AboutScanners />
        </div>
      </GlobalContainer>
    );
  }
}

const mapStateToProps = (state: Store) => {
  const customText = getGlobalSettingValue(state, 'sonar.lf.aboutText');
  return {
    appState: getAppState(state),
    currentUser: getCurrentUser(state),
    customText: customText && customText.value
  };
};

const mapDispatchToProps = { fetchAboutPageSettings } as any;

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AboutApp);
