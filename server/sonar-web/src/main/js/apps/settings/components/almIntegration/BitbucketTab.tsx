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
import {
  createBitbucketCloudConfiguration,
  createBitbucketConfiguration,
  updateBitbucketCloudConfiguration,
  updateBitbucketConfiguration
} from '../../../../api/alm-settings';
import {
  AlmKeys,
  AlmSettingsBindingStatus,
  BitbucketBindingDefinition,
  BitbucketCloudBindingDefinition,
  isBitbucketBindingDefinition
} from '../../../../types/alm-settings';
import BitbucketTabRenderer from './BitbucketTabRenderer';

interface Props {
  branchesEnabled: boolean;
  definitions: Array<BitbucketBindingDefinition | BitbucketCloudBindingDefinition>;
  definitionStatus: T.Dict<AlmSettingsBindingStatus>;
  loadingAlmDefinitions: boolean;
  loadingProjectCount: boolean;
  multipleAlmEnabled: boolean;
  onCheck: (definitionKey: string) => void;
  onDelete: (definitionKey: string) => void;
  onUpdateDefinitions: () => void;
}

interface State {
  editedDefinition?: BitbucketBindingDefinition | BitbucketCloudBindingDefinition;
  isCreating: boolean;
  submitting: boolean;
  success: boolean;
  variant?: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud;
}

export const DEFAULT_SERVER_BINDING = { key: '', url: '', personalAccessToken: '' };
export const DEFAULT_CLOUD_BINDING = { key: '', clientId: '', clientSecret: '', workspace: '' };

export default class BitbucketTab extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { isCreating: false, submitting: false, success: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleCancel = () => {
    this.setState({
      editedDefinition: undefined,
      isCreating: false,
      success: false,
      variant: undefined
    });
  };

  handleCreate = () => {
    this.setState({
      editedDefinition: DEFAULT_SERVER_BINDING, // Default to Bitbucket Server.
      isCreating: true,
      success: false,
      variant: undefined
    });
  };

  handleSelectVariant = (variant: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud) => {
    this.setState({
      variant,
      editedDefinition:
        variant === AlmKeys.BitbucketServer ? DEFAULT_SERVER_BINDING : DEFAULT_CLOUD_BINDING
    });
  };

  handleEdit = (definitionKey: string) => {
    const editedDefinition = this.props.definitions.find(d => d.key === definitionKey);
    const variant = isBitbucketBindingDefinition(editedDefinition)
      ? AlmKeys.BitbucketServer
      : AlmKeys.BitbucketCloud;
    this.setState({ editedDefinition, variant, success: false });
  };

  handleSubmit = (
    config: BitbucketBindingDefinition | BitbucketCloudBindingDefinition,
    originalKey: string
  ) => {
    const call = originalKey
      ? this.updateConfiguration({ newKey: config.key, ...config, key: originalKey })
      : this.createConfiguration({ ...config });

    this.setState({ submitting: true });
    return call
      .then(() => {
        if (this.mounted) {
          this.setState({
            editedDefinition: undefined,
            isCreating: false,
            submitting: false,
            success: true
          });
        }
      })
      .then(this.props.onUpdateDefinitions)
      .then(() => {
        this.props.onCheck(config.key);
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ submitting: false, success: false });
        }
      });
  };

  updateConfiguration = (
    config: (BitbucketBindingDefinition | BitbucketCloudBindingDefinition) & { newKey: string }
  ) => {
    if (isBitbucketBindingDefinition(config)) {
      return updateBitbucketConfiguration(config);
    }
    return updateBitbucketCloudConfiguration(config);
  };

  createConfiguration = (config: BitbucketBindingDefinition | BitbucketCloudBindingDefinition) => {
    if (isBitbucketBindingDefinition(config)) {
      return createBitbucketConfiguration(config);
    }
    return createBitbucketCloudConfiguration(config);
  };

  render() {
    const {
      branchesEnabled,
      definitions,
      definitionStatus,
      loadingAlmDefinitions,
      loadingProjectCount,
      multipleAlmEnabled
    } = this.props;
    const { editedDefinition, isCreating, submitting, success, variant } = this.state;

    return (
      <BitbucketTabRenderer
        branchesEnabled={branchesEnabled}
        definitions={definitions}
        definitionStatus={definitionStatus}
        editedDefinition={editedDefinition}
        isCreating={isCreating}
        loadingAlmDefinitions={loadingAlmDefinitions}
        loadingProjectCount={loadingProjectCount}
        multipleAlmEnabled={multipleAlmEnabled}
        onCancel={this.handleCancel}
        onCheck={this.props.onCheck}
        onCreate={this.handleCreate}
        onDelete={this.props.onDelete}
        onEdit={this.handleEdit}
        onSelectVariant={this.handleSelectVariant}
        onSubmit={this.handleSubmit}
        submitting={submitting}
        success={success}
        variant={variant}
      />
    );
  }
}
