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
import GitlabProjectCreateRenderer from './GitlabProjectCreateRenderer';

interface Props extends Pick<WithRouterProps, 'location'> {
  canAdmin: boolean;
  loadingBindings: boolean;
  onProjectCreate: (projectKeys: string[]) => void;
  settings: AlmSettingsInstance[];
}

interface State {
  loading: boolean;
  submittingToken: boolean;
  tokenIsValid: boolean;
  tokenValidationFailed: boolean;
  settings?: AlmSettingsInstance;
}

export default class GitlabProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false,
      tokenIsValid: false,
      settings: props.settings.length === 1 ? props.settings[0] : undefined,
      submittingToken: false,
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

    const tokenIsValid = await this.checkPersonalAccessToken();

    if (this.mounted) {
      this.setState({
        tokenIsValid,
        loading: false
      });
    }
  };

  checkPersonalAccessToken = () => {
    const { settings } = this.state;

    if (!settings) {
      return Promise.resolve(false);
    }

    return checkPersonalAccessTokenIsValid(settings.key).catch(() => false);
  };

  handlePersonalAccessTokenCreate = (token: string) => {
    const { settings } = this.state;

    if (!settings || token.length < 1) {
      return;
    }

    this.setState({ submittingToken: true, tokenValidationFailed: false });
    setAlmPersonalAccessToken(settings.key, token)
      .then(this.checkPersonalAccessToken)
      .then(patIsValid => {
        if (this.mounted) {
          this.setState({
            submittingToken: false,
            tokenIsValid: patIsValid,
            tokenValidationFailed: !patIsValid
          });
          if (patIsValid) {
            this.fetchInitialData();
          }
        }
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ submittingToken: false });
        }
      });
  };

  render() {
    const { canAdmin, loadingBindings, location } = this.props;
    const { loading, tokenIsValid, settings, submittingToken, tokenValidationFailed } = this.state;

    return (
      <GitlabProjectCreateRenderer
        settings={settings}
        canAdmin={canAdmin}
        loading={loading || loadingBindings}
        onPersonalAccessTokenCreate={this.handlePersonalAccessTokenCreate}
        showPersonalAccessTokenForm={!tokenIsValid || Boolean(location.query.resetPat)}
        submittingToken={submittingToken}
        tokenValidationFailed={tokenValidationFailed}
      />
    );
  }
}
