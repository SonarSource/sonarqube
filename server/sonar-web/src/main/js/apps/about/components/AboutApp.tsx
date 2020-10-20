/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { sanitize } from 'dompurify';
import { Location } from 'history';
import * as React from 'react';
import { connect } from 'react-redux';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { addWhitePageClass, removeWhitePageClass } from 'sonar-ui-common/helpers/pages';
import { searchProjects } from '../../../api/components';
import { getFacet } from '../../../api/issues';
import A11ySkipTarget from '../../../app/components/a11y/A11ySkipTarget';
import GlobalContainer from '../../../app/components/GlobalContainer';
import {
  getAppState,
  getCurrentUser,
  getGlobalSettingValue,
  Store
} from '../../../store/rootReducer';
import { fetchAboutPageSettings } from '../actions';
import '../styles.css';
import AboutCleanCode from './AboutCleanCode';
import AboutLanguages from './AboutLanguages';
import AboutLeakPeriod from './AboutLeakPeriod';
import AboutProjects from './AboutProjects';
import AboutQualityGates from './AboutQualityGates';
import AboutQualityModel from './AboutQualityModel';
import AboutScanners from './AboutScanners';
import AboutStandards from './AboutStandards';
import EntryIssueTypes from './EntryIssueTypes';

interface Props {
  appState: Pick<T.AppState, 'defaultOrganization' | 'organizationsEnabled'>;
  currentUser: T.CurrentUser;
  customText?: string;
  fetchAboutPageSettings: () => Promise<void>;
  location: Location;
}

interface State {
  issueTypes?: T.Dict<{ count: number }>;
  loading: boolean;
}

export class AboutApp extends React.PureComponent<Props, State> {
  mounted = false;

  state: State = {
    loading: true,
  };

  componentDidMount() {
    this.mounted = true;
    window.location.href = 'https://www.codescan.io/cloud/';
    document.body.classList.add('white-page');
    if (document.documentElement) {
      document.documentElement.classList.add('white-page');
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    removeWhitePageClass();
  }

  render() {
    return (<span></span>)
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

export default connect(mapStateToProps, mapDispatchToProps)(AboutApp);
