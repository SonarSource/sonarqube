/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { getAlmDefinitions } from '../../../../api/almSettings';
import { ALM_KEYS } from '../../utils';
import PRDecorationTabs from './PRDecorationTabs';

interface State {
  currentAlm: ALM_KEYS;
  definitions: T.AlmSettingsBindingDefinitions;
  loading: boolean;
}

export default class PullRequestDecoration extends React.PureComponent<{}, State> {
  mounted = false;
  state: State = {
    currentAlm: ALM_KEYS.GITHUB,
    definitions: {
      [ALM_KEYS.GITHUB]: []
    },
    loading: true
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchPullRequestDecorationSetting();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchPullRequestDecorationSetting = () => {
    return getAlmDefinitions()
      .then(definitions => {
        if (this.mounted) {
          this.setState({
            definitions,
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

  handleSelectAlm = (currentAlm: ALM_KEYS) => {
    this.setState({ currentAlm });
  };

  render() {
    return (
      <PRDecorationTabs
        onSelectAlm={this.handleSelectAlm}
        onUpdateDefinitions={this.fetchPullRequestDecorationSetting}
        {...this.state}
      />
    );
  }
}
