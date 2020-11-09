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
import { WithRouterProps } from 'react-router';
import {
  checkPersonalAccessTokenIsValid,
  setAlmPersonalAccessToken
} from '../../../api/alm-integrations';
import { AlmSettingsInstance } from '../../../types/alm-settings';
import AzureCreateProjectRenderer from './AzureProjectCreateRenderer';

interface Props extends Pick<WithRouterProps, 'location'> {
  canAdmin: boolean;
  loadingBindings: boolean;
  onProjectCreate: (projectKeys: string[]) => void;
  settings: AlmSettingsInstance[];
}

interface State {
  loading: boolean;
  patIsValid?: boolean;
  settings?: AlmSettingsInstance;
  submittingToken?: boolean;
  tokenValidationFailed: boolean;
}

export default class AzureProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      // For now, we only handle a single instance. So we always use the first
      // one from the list.
      settings: props.settings[0],
      loading: false,
      tokenValidationFailed: false
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchInitialData();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.settings.length === 0 && this.props.settings.length > 0) {
      this.setState(
        { settings: this.props.settings.length === 1 ? this.props.settings[0] : undefined },
        () => this.fetchInitialData()
      );
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchInitialData = async () => {
    this.setState({ loading: true });

    const patIsValid = await this.checkPersonalAccessToken().catch(() => false);

    if (this.mounted) {
      this.setState({
        patIsValid,
        loading: false
      });
    }
  };

  checkPersonalAccessToken = () => {
    const { settings } = this.state;

    if (!settings) {
      return Promise.resolve(false);
    }

    return checkPersonalAccessTokenIsValid(settings.key);
  };

  handlePersonalAccessTokenCreate = async (token: string) => {
    const { settings } = this.state;

    if (!settings || token.length < 1) {
      return;
    }

    this.setState({ submittingToken: true, tokenValidationFailed: false });

    try {
      await setAlmPersonalAccessToken(settings.key, token);
      const patIsValid = await this.checkPersonalAccessToken();

      if (this.mounted) {
        this.setState({ submittingToken: false, patIsValid, tokenValidationFailed: !patIsValid });

        if (patIsValid) {
          this.cleanUrl();
          await this.fetchInitialData();
        }
      }
    } catch (e) {
      if (this.mounted) {
        this.setState({ submittingToken: false });
      }
    }
  };

  render() {
    const { canAdmin, loadingBindings, location } = this.props;
    const { loading, patIsValid, settings, submittingToken, tokenValidationFailed } = this.state;

    return (
      <AzureCreateProjectRenderer
        canAdmin={canAdmin}
        loading={loading || loadingBindings}
        onPersonalAccessTokenCreate={this.handlePersonalAccessTokenCreate}
        settings={settings}
        showPersonalAccessTokenForm={!patIsValid || Boolean(location.query.resetPat)}
        submittingToken={submittingToken}
        tokenValidationFailed={tokenValidationFailed}
      />
    );
  }
}
