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
import {
  countBindedProjects,
  deleteConfiguration,
  getAlmDefinitions
} from '../../../../api/almSettings';
import { AlmSettingsBindingDefinitions, ALM_KEYS } from '../../../../types/alm-settings';
import PRDecorationTabs from './PRDecorationTabs';

interface State {
  currentAlm: ALM_KEYS;
  definitionKeyForDeletion?: string;
  definitions: AlmSettingsBindingDefinitions;
  loading: boolean;
  projectCount?: number;
}

export default class PullRequestDecoration extends React.PureComponent<{}, State> {
  mounted = false;
  state: State = {
    currentAlm: ALM_KEYS.GITHUB,
    definitions: {
      [ALM_KEYS.AZURE]: [],
      [ALM_KEYS.BITBUCKET]: [],
      [ALM_KEYS.GITHUB]: [],
      [ALM_KEYS.GITLAB]: []
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

  deleteConfiguration = (definitionKey: string) => {
    return deleteConfiguration(definitionKey)
      .then(() => {
        if (this.mounted) {
          this.setState({ definitionKeyForDeletion: undefined, projectCount: undefined });
        }
      })
      .then(this.fetchPullRequestDecorationSetting);
  };

  fetchPullRequestDecorationSetting = () => {
    this.setState({ loading: true });
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

  handleCancel = () => {
    this.setState({ definitionKeyForDeletion: undefined, projectCount: undefined });
  };

  handleDelete = (definitionKey: string) => {
    this.setState({ loading: true });
    return countBindedProjects(definitionKey)
      .then(projectCount => {
        if (this.mounted) {
          this.setState({
            definitionKeyForDeletion: definitionKey,
            loading: false,
            projectCount
          });
        }
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      });
  };

  render() {
    return (
      <PRDecorationTabs
        onCancel={this.handleCancel}
        onConfirmDelete={this.deleteConfiguration}
        onDelete={this.handleDelete}
        onSelectAlm={this.handleSelectAlm}
        onUpdateDefinitions={this.fetchPullRequestDecorationSetting}
        {...this.state}
      />
    );
  }
}
