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
import { find, without } from 'lodash';
import * as React from 'react';
import Modal from 'sonar-ui-common/components/controls/Modal';
import SelectList, {
  SelectListFilter,
  SelectListSearchParams
} from 'sonar-ui-common/components/controls/SelectList';
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  associateProject,
  dissociateProject,
  getProfileProjects,
  ProfileProject
} from '../../../api/quality-profiles';
import { Profile } from '../types';

interface Props {
  onClose: () => void;
  organization: string | null;
  profile: Profile;
}

interface State {
  needToReload: boolean;
  lastSearchParams?: SelectListSearchParams;
  projects: ProfileProject[];
  projectsTotalCount?: number;
  selectedProjects: string[];
}

export default class ChangeProjectsForm extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      needToReload: false,
      projects: [],
      selectedProjects: []
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchProjects = (searchParams: SelectListSearchParams) =>
    getProfileProjects({
      key: this.props.profile.key,
      organization: this.props.organization,
      p: searchParams.page,
      ps: searchParams.pageSize,
      q: searchParams.query !== '' ? searchParams.query : undefined,
      selected: searchParams.filter
    }).then(data => {
      if (this.mounted) {
        this.setState(prevState => {
          const more = searchParams.page != null && searchParams.page > 1;

          const projects = more ? [...prevState.projects, ...data.results] : data.results;
          const newSeletedProjects = data.results
            .filter(project => project.selected)
            .map(project => project.key);
          const selectedProjects = more
            ? [...prevState.selectedProjects, ...newSeletedProjects]
            : newSeletedProjects;

          return {
            lastSearchParams: searchParams,
            needToReload: false,
            projects,
            projectsTotalCount: data.paging.total,
            selectedProjects
          };
        });
      }
    });

  handleSelect = (key: string) =>
    associateProject(this.props.profile.key, key).then(() => {
      if (this.mounted) {
        this.setState((state: State) => ({
          needToReload: true,
          selectedProjects: [...state.selectedProjects, key]
        }));
      }
    });

  handleUnselect = (key: string) =>
    dissociateProject(this.props.profile.key, key).then(() => {
      if (this.mounted) {
        this.setState((state: State) => ({
          needToReload: true,
          selectedProjects: without(state.selectedProjects, key)
        }));
      }
    });

  handleCloseClick = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  renderElement = (key: string): React.ReactNode => {
    const project = find(this.state.projects, { key });
    return (
      <div className="select-list-list-item">
        {project === undefined ? (
          key
        ) : (
          <>
            {project.name}
            <br />
            <span className="note">{project.key}</span>
          </>
        )}
      </div>
    );
  };

  render() {
    const header = translate('projects');

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <div className="modal-head">
          <h2>{header}</h2>
        </div>

        <div className="modal-body modal-container" id="profile-projects">
          <SelectList
            allowBulkSelection={true}
            elements={this.state.projects.map(project => project.key)}
            elementsTotalCount={this.state.projectsTotalCount}
            labelAll={translate('quality_gates.projects.all')}
            labelSelected={translate('quality_gates.projects.with')}
            labelUnselected={translate('quality_gates.projects.without')}
            needToReload={
              this.state.needToReload &&
              this.state.lastSearchParams &&
              this.state.lastSearchParams.filter !== SelectListFilter.All
            }
            onSearch={this.fetchProjects}
            onSelect={this.handleSelect}
            onUnselect={this.handleUnselect}
            renderElement={this.renderElement}
            selectedElements={this.state.selectedProjects}
            withPaging={true}
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
