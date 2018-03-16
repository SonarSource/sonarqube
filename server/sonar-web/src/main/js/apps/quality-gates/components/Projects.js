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
import { find, without } from 'lodash';
import SelectList, { Filter } from '../../../components/SelectList/SelectList';
import { translate } from '../../../helpers/l10n';
import {
  searchGates,
  associateGateWithProject,
  dissociateGateWithProject
} from '../../../api/quality-gates';
/*:: import { Project } from '../../projects/types'; */

/*::
type State = {
  projects: Projects[],
  selectedProjects: string[]
};
*/

export default class Projects extends React.PureComponent {
  state /*: State */ = { projects: [], selectedProjects: [] };

  componentDidMount() {
    this.handleSearch('', Filter.Selected);
  }

  handleSearch = (query /*: string*/, selected /*: string */) => {
    return searchGates({
      gateId: this.props.qualityGate.id,
      organization: this.props.organization,
      pageSize: 100,
      query: query !== '' ? query : undefined,
      selected
    }).then(data => {
      this.setState({
        projects: data.results,
        selectedProjects: data.results
          .filter(project => project.selected)
          .map(project => project.id)
      });
    });
  };

  handleSelect = (id /*: string*/) => {
    return associateGateWithProject({
      gateId: this.props.qualityGate.id,
      organization: this.props.organization,
      projectId: id
    }).then(() => {
      this.setState((state /*: State*/) => ({
        selectedProjects: [...state.selectedProjects, id]
      }));
    });
  };

  handleUnselect = (id /*: string*/) => {
    return dissociateGateWithProject({
      gateId: this.props.qualityGate.id,
      organization: this.props.organization,
      projectId: id
    }).then(
      () => {
        this.setState((state /*: State*/) => ({
          selectedProjects: without(state.selectedProjects, id)
        }));
      },
      () => {}
    );
  };

  renderElement = (id /*: string*/) /*: React.ReactNode*/ => {
    const project = find(this.state.projects, { id });
    return project === undefined ? id : project.name;
  };

  render() {
    return (
      <SelectList
        elements={this.state.projects.map(project => project.id)}
        labelAll={translate('quality_gates.projects.all')}
        labelSelected={translate('quality_gates.projects.with')}
        labelUnselected={translate('quality_gates.projects.without')}
        onSearch={this.handleSearch}
        onSelect={this.handleSelect}
        onUnselect={this.handleUnselect}
        renderElement={this.renderElement}
        selectedElements={this.state.selectedProjects}
      />
    );
  }
}
