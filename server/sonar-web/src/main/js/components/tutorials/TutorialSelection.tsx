/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { WithRouterProps } from 'react-router';
import { getAlmSettingsNoCatch } from '../../api/alm-settings';
import { getValues } from '../../api/settings';
import { getHostUrl } from '../../helpers/urls';
import { AlmSettingsInstance, ProjectAlmBindingResponse } from '../../types/alm-settings';
import { SettingsKey } from '../../types/settings';
import { Component, LoggedInUser } from '../../types/types';
import { withRouter } from '../hoc/withRouter';
import TutorialSelectionRenderer from './TutorialSelectionRenderer';
import { TutorialModes } from './types';

interface Props extends Pick<WithRouterProps, 'router' | 'location'> {
  component: Component;
  currentUser: LoggedInUser;
  projectBinding?: ProjectAlmBindingResponse;
  willRefreshAutomatically?: boolean;
}

interface State {
  almBinding?: AlmSettingsInstance;
  baseUrl: string;
  loading: boolean;
}

export class TutorialSelection extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    baseUrl: getHostUrl(),
    loading: true
  };

  async componentDidMount() {
    this.mounted = true;

    await Promise.all([this.fetchAlmBindings(), this.fetchBaseUrl()]);

    if (this.mounted) {
      this.setState({ loading: false });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchAlmBindings = async () => {
    const { component, projectBinding } = this.props;

    if (projectBinding !== undefined) {
      const almSettings = await getAlmSettingsNoCatch(component.key).catch(() => undefined);
      if (this.mounted) {
        let almBinding;
        if (almSettings !== undefined) {
          almBinding = almSettings.find(d => d.key === projectBinding.key);
        }
        this.setState({ almBinding });
      }
    }
  };

  fetchBaseUrl = async () => {
    const settings = await getValues({ keys: SettingsKey.ServerBaseUrl }).catch(() => undefined);
    const baseUrl = settings && settings.find(s => s.key === SettingsKey.ServerBaseUrl)?.value;
    if (baseUrl && baseUrl.length > 0 && this.mounted) {
      this.setState({ baseUrl });
    }
  };

  handleSelectTutorial = (selectedTutorial: TutorialModes) => {
    const {
      router,
      location: { pathname, query }
    } = this.props;

    router.push({
      pathname,
      query: { ...query, selectedTutorial }
    });
  };

  render() {
    const {
      component,
      currentUser,
      location,
      projectBinding,
      willRefreshAutomatically
    } = this.props;
    const { almBinding, baseUrl, loading } = this.state;

    const selectedTutorial: TutorialModes | undefined = location.query?.selectedTutorial;

    return (
      <TutorialSelectionRenderer
        almBinding={almBinding}
        baseUrl={baseUrl}
        component={component}
        currentUser={currentUser}
        loading={loading}
        onSelectTutorial={this.handleSelectTutorial}
        projectBinding={projectBinding}
        selectedTutorial={selectedTutorial}
        willRefreshAutomatically={willRefreshAutomatically}
      />
    );
  }
}

export default withRouter(TutorialSelection);
