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
import { createGitlabConfiguration, updateGitlabConfiguration } from '../../../../api/almSettings';
import { GitlabBindingDefinition } from '../../../../types/alm-settings';
import GitlabTabRenderer from './GitlabTabRenderer';

interface Props {
  definitions: GitlabBindingDefinition[];
  loading: boolean;
  onDelete: (definitionKey: string) => void;
  onUpdateDefinitions: () => void;
}

interface State {
  editedDefinition?: GitlabBindingDefinition;
  projectCount?: number;
}

export default class GitlabTab extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleEdit = (definitionKey: string) => {
    const editedDefinition = this.props.definitions.find(d => d.key === definitionKey);
    this.setState({ editedDefinition });
  };

  handleSubmit = (config: GitlabBindingDefinition, originalKey: string) => {
    const call = originalKey
      ? updateGitlabConfiguration({ newKey: config.key, ...config, key: originalKey })
      : createGitlabConfiguration(config);
    return call.then(this.props.onUpdateDefinitions).then(() => {
      if (this.mounted) {
        this.setState({ editedDefinition: undefined });
      }
    });
  };

  handleCancel = () => {
    this.setState({
      editedDefinition: undefined
    });
  };

  handleCreate = () => {
    this.setState({ editedDefinition: { key: '', personalAccessToken: '' } });
  };

  render() {
    const { definitions, loading } = this.props;
    const { editedDefinition } = this.state;
    return (
      <GitlabTabRenderer
        definitions={definitions}
        editedDefinition={editedDefinition}
        loading={loading}
        onCancel={this.handleCancel}
        onCreate={this.handleCreate}
        onDelete={this.props.onDelete}
        onEdit={this.handleEdit}
        onSubmit={this.handleSubmit}
      />
    );
  }
}
