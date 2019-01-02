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
import { find, without } from 'lodash';
import Modal from '../../../components/controls/Modal';
import SelectList, { Filter } from '../../../components/SelectList/SelectList';
import { translate } from '../../../helpers/l10n';
import { Profile } from '../types';
import {
  getProfileProjects,
  associateProject,
  dissociateProject,
  ProfileProject
} from '../../../api/quality-profiles';

interface Props {
  onClose: () => void;
  organization: string | null;
  profile: Profile;
}

interface State {
  projects: ProfileProject[];
  selectedProjects: string[];
}

export default class ChangeProjectsForm extends React.PureComponent<Props> {
  container?: HTMLElement | null;
  state: State = { projects: [], selectedProjects: [] };

  componentDidMount() {
    this.handleSearch('', Filter.Selected);
  }

  handleSearch = (query: string, selected: Filter) => {
    return getProfileProjects({
      key: this.props.profile.key,
      organization: this.props.organization,
      pageSize: 100,
      query: query !== '' ? query : undefined,
      selected
    }).then(
      data => {
        this.setState({
          projects: data.results,
          selectedProjects: data.results
            .filter(project => project.selected)
            .map(project => project.key)
        });
      },
      () => {}
    );
  };

  handleSelect = (key: string) => {
    return associateProject(this.props.profile.key, key).then(() => {
      this.setState((state: State) => ({
        selectedProjects: [...state.selectedProjects, key]
      }));
    });
  };

  handleUnselect = (key: string) => {
    return dissociateProject(this.props.profile.key, key).then(() => {
      this.setState((state: State) => ({ selectedProjects: without(state.selectedProjects, key) }));
    });
  };

  handleCloseClick = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  renderElement = (key: string): React.ReactNode => {
    const project = find(this.state.projects, { key });
    return project === undefined ? key : project.name;
  };

  render() {
    const header = translate('projects');

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <div className="modal-head">
          <h2>{header}</h2>
        </div>

        <div className="modal-body" id="profile-projects">
          <SelectList
            allowBulkSelection={true}
            elements={this.state.projects.map(project => project.key)}
            labelAll={translate('quality_gates.projects.all')}
            labelSelected={translate('quality_gates.projects.with')}
            labelUnselected={translate('quality_gates.projects.without')}
            onSearch={this.handleSearch}
            onSelect={this.handleSelect}
            onUnselect={this.handleUnselect}
            renderElement={this.renderElement}
            selectedElements={this.state.selectedProjects}
          />
        </div>

        <div className="modal-foot">
          <a href="#" onClick={this.handleCloseClick}>
            {translate('close')}
          </a>
        </div>
      </Modal>
    );
  }
}
