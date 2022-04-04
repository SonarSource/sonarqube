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
import {
  countBindedProjects,
  deleteConfiguration,
  getAlmDefinitions,
  validateAlmSettings
} from '../../../../api/alm-settings';
import { withAppState } from '../../../../components/hoc/withAppState';
import { withRouter } from '../../../../components/hoc/withRouter';
import {
  AlmBindingDefinition,
  AlmKeys,
  AlmSettingsBindingDefinitions,
  AlmSettingsBindingStatus,
  AlmSettingsBindingStatusType
} from '../../../../types/alm-settings';
import AlmIntegrationRenderer from './AlmIntegrationRenderer';
import { ALM_KEY_LIST } from './utils';

interface Props extends Pick<WithRouterProps, 'location'> {
  appState: Pick<T.AppState, 'branchesEnabled' | 'multipleAlmEnabled'>;
  component?: T.Component;
}

interface State {
  currentAlm: AlmKeys;
  definitionKeyForDeletion?: string;
  definitions: AlmSettingsBindingDefinitions;
  definitionStatus: T.Dict<AlmSettingsBindingStatus>;
  loadingAlmDefinitions: boolean;
  loadingProjectCount: boolean;
  projectCount?: number;
}

export class AlmIntegration extends React.PureComponent<Props, State> {
  mounted = false;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      currentAlm: props.location.query.alm || AlmKeys.GitHub,
      definitions: {
        [AlmKeys.Azure]: [],
        [AlmKeys.BitbucketServer]: [],
        [AlmKeys.BitbucketCloud]: [],
        [AlmKeys.GitHub]: [],
        [AlmKeys.GitLab]: []
      },
      definitionStatus: {},
      loadingAlmDefinitions: true,
      loadingProjectCount: false
    };
  }

  componentDidMount() {
    this.mounted = true;
    return this.fetchPullRequestDecorationSetting().then(definitions => {
      if (definitions) {
        // Validate all alms on load:
        ALM_KEY_LIST.forEach(alm => {
          this.state.definitions[alm].forEach((def: AlmBindingDefinition) =>
            this.handleCheck(def.key, false)
          );
        });
      }
    });
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
    this.setState({ loadingAlmDefinitions: true });
    return getAlmDefinitions()
      .then(definitions => {
        if (this.mounted) {
          this.setState({
            definitions,
            loadingAlmDefinitions: false
          });
          return definitions;
        }
        return undefined;
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ loadingAlmDefinitions: false });
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
    this.setState({ loadingProjectCount: true });
    return countBindedProjects(definitionKey)
      .then(projectCount => {
        if (this.mounted) {
          this.setState({
            definitionKeyForDeletion: definitionKey,
            loadingProjectCount: false,
            projectCount
          });
        }
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ loadingProjectCount: false });
        }
      });
  };

  handleCheck = async (definitionKey: string, alertSuccess = true) => {
    this.setState(({ definitionStatus }) => {
      definitionStatus[definitionKey] = {
        ...definitionStatus[definitionKey],
        type: AlmSettingsBindingStatusType.Validating
      };

      return { definitionStatus: { ...definitionStatus } };
    });

    let type: AlmSettingsBindingStatusType;
    let failureMessage = '';

    try {
      failureMessage = await validateAlmSettings(definitionKey);
      type = failureMessage
        ? AlmSettingsBindingStatusType.Failure
        : AlmSettingsBindingStatusType.Success;
    } catch (_) {
      type = AlmSettingsBindingStatusType.Warning;
    }

    if (this.mounted) {
      this.setState(({ definitionStatus }) => {
        definitionStatus[definitionKey] = {
          alertSuccess,
          failureMessage,
          type
        };

        return { definitionStatus: { ...definitionStatus } };
      });
    }
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
        onCheck={this.handleCheck}
        onDelete={this.handleDelete}
        onSelectAlm={this.handleSelectAlm}
        onUpdateDefinitions={this.fetchPullRequestDecorationSetting}
        {...this.state}
      />
    );
  }
}

export default withRouter(withAppState(AlmIntegration));
