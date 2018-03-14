/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import React from 'react';
import SelectList from '../../../components/SelectList/SelectList';
import { translate } from '../../../helpers/l10n';
import {
  searchGates,
  associateGateWithProject,
  dissociateGateWithProject
} from '../../../api/quality-gates';

/*::
type State = {
  projects: Array<{ id: number; name: string; selected: boolean }>
};
*/

export default class Projects extends React.PureComponent {
  state /*: State */ = {
    projects: []
  };

  componentDidMount() {
    this.handleSearch('', 'selected');
  }

  handleSearch = (query /*: string*/, selected /*: string */) => {
    const { qualityGate, organization } = this.props;
    const requestData = {
      gateId: qualityGate.id,
      pageSize: 100,
      selected
    };

    if (query !== '') {
      requestData.query = query;
    }

    if (organization) {
      requestData.organization = organization;
    }

    return searchGates(requestData).then(
      data => {
        this.setState({
          projects: data.results.map(project => {
            return { key: project.id, name: project.name, selected: project.selected };
          })
        });
      },
      () => {}
    );
  };

  handleSelect = (key /*: number|string*/) => {
    const { qualityGate, organization } = this.props;
    const requestData = {
      gateId: qualityGate.id,
      projectId: parseInt(key, 10)
    };

    if (organization) {
      requestData.organization = organization;
    }

    return associateGateWithProject(requestData).then(
      () => {
        this.setState(state => {
          const projects = state.projects.map(project => {
            return project.key === key ? { ...project, selected: true } : project;
          });
          return { projects };
        });
      },
      () => {}
    );
  };

  handleUnselect = (key /*: number|string*/) => {
    const { qualityGate, organization } = this.props;
    const requestData = {
      gateId: qualityGate.id,
      projectId: parseInt(key, 10)
    };

    if (organization) {
      requestData.organization = organization;
    }

    return dissociateGateWithProject(requestData).then(
      () => {
        this.setState(state => {
          const projects = state.projects.map(project => {
            return project.key === key ? { ...project, selected: false } : project;
          });
          return { projects };
        });
      },
      () => {}
    );
  };

  render() {
    return (
      <SelectList
        elements={this.state.projects}
        labelAll={translate('quality_gates.projects.all')}
        labelDeselected={translate('quality_gates.projects.without')}
        labelSelected={translate('quality_gates.projects.with')}
        onSearch={this.handleSearch}
        onSelect={this.handleSelect}
        onUnselect={this.handleUnselect}
      />
    );
  }
}
