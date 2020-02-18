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
import { AlmBindingDefinition, AlmKeys } from '../../../../types/alm-settings';
import { AlmBindingDefinitionFormChildrenProps } from './AlmBindingDefinitionForm';
import { AlmIntegrationFeatureBoxProps } from './AlmIntegrationFeatureBox';
import AlmTabRenderer from './AlmTabRenderer';

interface Props<B> {
  alm: AlmKeys;
  additionalColumnsHeaders?: string[];
  additionalColumnsKeys?: Array<keyof B>;
  additionalTableInfo?: React.ReactNode;
  createConfiguration: (data: B) => Promise<void>;
  defaultBinding: B;
  definitions: B[];
  features?: AlmIntegrationFeatureBoxProps[];
  form: (props: AlmBindingDefinitionFormChildrenProps<B>) => React.ReactNode;
  help?: React.ReactNode;
  loading: boolean;
  multipleAlmEnabled: boolean;
  onDelete: (definitionKey: string) => void;
  onUpdateDefinitions: () => void;
  optionalFields?: Array<keyof B>;
  updateConfiguration: (data: B & { newKey?: string }) => Promise<void>;
}

interface State<B> {
  editedDefinition?: B;
  submitting: boolean;
  success: boolean;
}

export default class AlmTab<B extends AlmBindingDefinition> extends React.PureComponent<
  Props<B>,
  State<B>
> {
  mounted = false;
  state: State<B> = { submitting: false, success: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleCancel = () => {
    this.setState({
      editedDefinition: undefined,
      success: false
    });
  };

  handleCreate = () => {
    this.setState({ editedDefinition: this.props.defaultBinding, success: false });
  };

  handleEdit = (definitionKey: string) => {
    const editedDefinition = this.props.definitions.find(d => d.key === definitionKey);
    this.setState({ editedDefinition, success: false });
  };

  handleSubmit = (config: B, originalKey: string) => {
    const call = originalKey
      ? this.props.updateConfiguration({ newKey: config.key, ...config, key: originalKey })
      : // If there's no support for multi-ALM binding, the key will be an empty string.
        // Set a default.
        this.props.createConfiguration(config.key ? config : { ...config, key: this.props.alm });

    this.setState({ submitting: true });
    return call
      .then(() => {
        if (this.mounted) {
          this.setState({ editedDefinition: undefined, submitting: false, success: true });
        }
      })
      .then(this.props.onUpdateDefinitions)
      .catch(() => {
        if (this.mounted) {
          this.setState({ submitting: false, success: false });
        }
      });
  };

  render() {
    const {
      additionalColumnsHeaders = [],
      additionalColumnsKeys = [],
      additionalTableInfo,
      alm,
      defaultBinding,
      definitions,
      features,
      form,
      help,
      loading,
      multipleAlmEnabled,
      optionalFields
    } = this.props;
    const { editedDefinition, submitting, success } = this.state;

    return (
      <AlmTabRenderer
        additionalColumnsHeaders={additionalColumnsHeaders}
        additionalColumnsKeys={additionalColumnsKeys}
        additionalTableInfo={additionalTableInfo}
        alm={alm}
        defaultBinding={defaultBinding}
        definitions={definitions}
        editedDefinition={editedDefinition}
        features={features}
        form={form}
        help={help}
        loading={loading || submitting}
        multipleAlmEnabled={multipleAlmEnabled}
        onCancel={this.handleCancel}
        onCreate={this.handleCreate}
        onDelete={this.props.onDelete}
        onEdit={this.handleEdit}
        onSubmit={this.handleSubmit}
        optionalFields={optionalFields}
        success={success}
      />
    );
  }
}
