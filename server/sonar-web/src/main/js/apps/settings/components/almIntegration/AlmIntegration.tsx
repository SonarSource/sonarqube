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
} from '../../../../api/alm-settings';
import { withAppState } from '../../../../components/hoc/withAppState';
import { AlmKeys, AlmSettingsBindingDefinitions } from '../../../../types/alm-settings';
import AlmIntegrationRenderer from './AlmIntegrationRenderer';

interface Props {
  appState: Pick<T.AppState, 'branchesEnabled' | 'multipleAlmEnabled'>;
  component?: T.Component;
}

interface State {
  currentAlm: AlmKeys;
  definitionKeyForDeletion?: string;
  definitions: AlmSettingsBindingDefinitions;
  loading: boolean;
  projectCount?: number;
}

export class AlmIntegration extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    currentAlm: AlmKeys.GitHub,
    definitions: {
      [AlmKeys.Azure]: [],
      [AlmKeys.Bitbucket]: [],
      [AlmKeys.GitHub]: [],
      [AlmKeys.GitLab]: []
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
    const {
      appState: { branchesEnabled }
    } = this.props;

    if (!branchesEnabled) {
      return Promise.resolve();
    }

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

  handleSelectAlm = (currentAlm: AlmKeys) => {
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
    const {
      appState: { branchesEnabled, multipleAlmEnabled },
      component
    } = this.props;
    return (
      <AlmIntegrationRenderer
        branchesEnabled={Boolean(branchesEnabled)}
        component={component}
        multipleAlmEnabled={Boolean(multipleAlmEnabled)}
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

export default withAppState(AlmIntegration);
