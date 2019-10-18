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
import {
  countBindedProjects,
  createGithubConfiguration,
  deleteConfiguration,
  updateGithubConfiguration
} from '../../../../api/almSettings';
import { ALM_KEYS } from '../../utils';
import TabRenderer from './TabRenderer';

interface Props {
  definitions: T.GithubBindingDefinition[];
  onUpdateDefinitions: () => void;
}

interface State {
  definitionInEdition?: T.GithubBindingDefinition;
  definitionKeyForDeletion?: string;
  projectCount?: number;
}

export default class GithubTab extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleCancel = () => {
    this.setState({
      definitionKeyForDeletion: undefined,
      definitionInEdition: undefined,
      projectCount: undefined
    });
  };

  deleteConfiguration = (id: string) => {
    return deleteConfiguration(id)
      .then(this.props.onUpdateDefinitions)
      .then(() => {
        if (this.mounted) {
          this.setState({ definitionKeyForDeletion: undefined });
        }
      });
  };

  handleCreate = () => {
    this.setState({ definitionInEdition: { key: '', appId: '', url: '', privateKey: '' } });
  };

  handleDelete = (config: T.GithubBindingDefinition) => {
    this.setState({ definitionKeyForDeletion: config.key });

    return countBindedProjects(config.key).then(projectCount => {
      if (this.mounted) {
        this.setState({ projectCount });
      }
    });
  };

  handleEdit = (config: T.GithubBindingDefinition) => {
    this.setState({ definitionInEdition: config });
  };

  handleSubmit = (config: T.GithubBindingDefinition, originalKey: string) => {
    const call = originalKey
      ? updateGithubConfiguration({ newKey: config.key, ...config, key: originalKey })
      : createGithubConfiguration(config);
    return call.then(this.props.onUpdateDefinitions).then(() => {
      if (this.mounted) {
        this.setState({ definitionInEdition: undefined });
      }
    });
  };

  render() {
    const { definitions } = this.props;
    const { definitionKeyForDeletion, definitionInEdition, projectCount } = this.state;
    return (
      <TabRenderer
        alm={ALM_KEYS.GITHUB}
        definitionInEdition={definitionInEdition}
        definitionKeyForDeletion={definitionKeyForDeletion}
        definitions={definitions}
        onCancel={this.handleCancel}
        onConfirmDelete={this.deleteConfiguration}
        onCreate={this.handleCreate}
        onDelete={this.handleDelete}
        onEdit={this.handleEdit}
        onSubmit={this.handleSubmit}
        projectCount={projectCount}
      />
    );
  }
}
