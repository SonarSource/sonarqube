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
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { WithRouterProps } from 'react-router';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { addWhitePageClass, removeWhitePageClass } from 'sonar-ui-common/helpers/pages';
import { getAlmSettings } from '../../../api/alm-settings';
import { whenLoggedIn } from '../../../components/hoc/whenLoggedIn';
import { withAppState } from '../../../components/hoc/withAppState';
import { getProjectUrl } from '../../../helpers/urls';
import { AlmKeys, AlmSettingsInstance } from '../../../types/alm-settings';
import BitbucketProjectCreate from './BitbucketProjectCreate';
import CreateProjectModeSelection from './CreateProjectModeSelection';
import ManualProjectCreate from './ManualProjectCreate';
import './style.css';
import { CreateProjectModes } from './types';

interface Props extends Pick<WithRouterProps, 'router' | 'location'> {
  appState: Pick<T.AppState, 'branchesEnabled'>;
  currentUser: T.LoggedInUser;
}

interface State {
  bitbucketSettings: AlmSettingsInstance[];
  loading: boolean;
}

export class CreateProjectPageSonarQube extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { bitbucketSettings: [], loading: false };

  componentDidMount() {
    const {
      appState: { branchesEnabled },
      location
    } = this.props;
    this.mounted = true;
    if (branchesEnabled) {
      this.fetchAlmBindings();
    }

    if (location.query?.mode || !branchesEnabled) {
      addWhitePageClass();
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.location.query?.mode && !prevProps.location.query?.mode) {
      addWhitePageClass();
    } else if (!this.props.location.query?.mode && prevProps.location.query?.mode) {
      removeWhitePageClass();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    removeWhitePageClass();
  }

  fetchAlmBindings = () => {
    this.setState({ loading: true });
    getAlmSettings()
      .then(almSettings => {
        if (this.mounted) {
          this.setState({
            bitbucketSettings: almSettings.filter(s => s.alm === AlmKeys.Bitbucket),
            loading: false
          });
        }
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      });
  };

  handleProjectCreate = (projectKeys: string[]) => {
    if (projectKeys.length === 1) {
      this.props.router.push(getProjectUrl(projectKeys[0]));
    }
  };

  handleModeSelect = (mode: CreateProjectModes) => {
    const { router, location } = this.props;
    router.push({
      pathname: location.pathname,
      query: { mode }
    });
  };

  render() {
    const {
      appState: { branchesEnabled },
      currentUser,
      location
    } = this.props;
    const { bitbucketSettings, loading } = this.state;

    const mode: CreateProjectModes | undefined = location.query?.mode;
    const showManualForm = !branchesEnabled || mode === CreateProjectModes.Manual;
    const showBBSForm = branchesEnabled && mode === CreateProjectModes.BitbucketServer;

    return (
      <>
        <Helmet title={translate('my_account.create_new.TRK')} titleTemplate="%s" />
        <div className="page page-limited huge-spacer-bottom position-relative" id="create-project">
          {!showBBSForm && !showManualForm && (
            <CreateProjectModeSelection
              bbsBindingCount={bitbucketSettings.length}
              loadingBindings={loading}
              onSelectMode={this.handleModeSelect}
            />
          )}

          {showManualForm && (
            <ManualProjectCreate
              branchesEnabled={branchesEnabled}
              currentUser={currentUser}
              onProjectCreate={this.handleProjectCreate}
            />
          )}

          {showBBSForm && (
            <BitbucketProjectCreate
              bitbucketSettings={bitbucketSettings}
              loadingBindings={loading}
              location={location}
              onProjectCreate={this.handleProjectCreate}
            />
          )}
        </div>
      </>
    );
  }
}

export default whenLoggedIn(withAppState(CreateProjectPageSonarQube));
