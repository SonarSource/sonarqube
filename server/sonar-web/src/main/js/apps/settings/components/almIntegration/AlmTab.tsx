/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
  AlmBindingDefinition,
  AlmBindingDefinitionBase,
  AlmSettingsBindingStatus,
} from '../../../../types/alm-settings';
import { Dict } from '../../../../types/types';
import { AlmTabs } from './AlmIntegration';
import AlmTabRenderer from './AlmTabRenderer';

interface Props {
  almTab: AlmTabs;
  branchesEnabled: boolean;
  definitionStatus: Dict<AlmSettingsBindingStatus>;
  definitions: AlmBindingDefinition[];
  loadingAlmDefinitions: boolean;
  loadingProjectCount: boolean;
  multipleAlmEnabled: boolean;
  onCheck: (definitionKey: string) => void;
  onDelete: (definitionKey: string) => void;
  onUpdateDefinitions: () => void;
}

interface State {
  editDefinition?: boolean;
  editedDefinition?: AlmBindingDefinition;
}

export default class AlmTab extends React.PureComponent<Props, State> {
  state: State = {};
  mounted = false;

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleCancel = () => {
    this.setState({ editDefinition: false, editedDefinition: undefined });
  };

  handleCreate = () => {
    this.setState({ editDefinition: true, editedDefinition: undefined });
  };

  handleEdit = (definitionKey: string) => {
    const editedDefinition = this.props.definitions.find((d) => d.key === definitionKey);
    this.setState({ editDefinition: true, editedDefinition });
  };

  handleAfterSubmit = (config: AlmBindingDefinitionBase) => {
    if (this.mounted) {
      this.setState({
        editDefinition: false,
        editedDefinition: undefined,
      });
    }

    this.props.onUpdateDefinitions();

    this.props.onCheck(config.key);
  };

  render() {
    const {
      almTab,
      branchesEnabled,
      definitions,
      definitionStatus,
      loadingAlmDefinitions,
      loadingProjectCount,
      multipleAlmEnabled,
    } = this.props;
    const { editDefinition, editedDefinition } = this.state;

    return (
      <AlmTabRenderer
        almTab={almTab}
        branchesEnabled={branchesEnabled}
        definitions={definitions}
        definitionStatus={definitionStatus}
        editDefinition={editDefinition}
        editedDefinition={editedDefinition}
        loadingAlmDefinitions={loadingAlmDefinitions}
        loadingProjectCount={loadingProjectCount}
        multipleAlmEnabled={multipleAlmEnabled}
        onCheck={this.props.onCheck}
        onCreate={this.handleCreate}
        onDelete={this.props.onDelete}
        onEdit={this.handleEdit}
        onCancel={this.handleCancel}
        afterSubmit={this.handleAfterSubmit}
      />
    );
  }
}
