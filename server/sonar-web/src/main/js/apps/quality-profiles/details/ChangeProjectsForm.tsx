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
import {
  associateProject,
  dissociateProject,
  getProfileProjects,
  ProfileProject
} from '../../../api/quality-profiles';
import Modal from '../../../components/controls/Modal';
import SelectList, { Filter } from '../../../components/SelectList/SelectList';
import { translate } from '../../../helpers/l10n';
import { Profile } from '../types';

interface Props {
  onClose: () => void;
  organization: string | null;
  profile: Profile;
}

export interface SearchParams {
  key: string;
  organization: string | null;
  page: number;
  pageSize: number;
  query?: string;
  selected: string;
}

interface State {
  lastSearchParams: SearchParams;
  listHasBeenTouched: boolean;
  projects: ProfileProject[];
  projectsTotalCount?: number;
  selectedProjects: string[];
}

const PAGE_SIZE = 100;

export default class ChangeProjectsForm extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      lastSearchParams: {
        key: props.profile.key,
        organization: props.organization,
        page: 1,
        pageSize: PAGE_SIZE,
        query: '',
        selected: Filter.Selected
      },
      listHasBeenTouched: false,
      projects: [],
      selectedProjects: []
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchProjects(this.state.lastSearchParams);
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchProjects = (searchParams: SearchParams, more?: boolean) =>
    getProfileProjects({
      ...searchParams,
      p: searchParams.page,
      ps: searchParams.pageSize,
      q: searchParams.query !== '' ? searchParams.query : undefined
    }).then(data => {
      if (this.mounted) {
        this.setState(prevState => {
          const projects = more ? [...prevState.projects, ...data.results] : data.results;
          const newSeletedProjects = data.results
            .filter(project => project.selected)
            .map(project => project.key);
          const selectedProjects = more
            ? [...prevState.selectedProjects, ...newSeletedProjects]
            : newSeletedProjects;

          return {
            lastSearchParams: searchParams,
            listHasBeenTouched: false,
            projects,
            projectsTotalCount: data.paging.total,
            selectedProjects
          };
        });
      }
    });

  handleLoadMore = () =>
    this.fetchProjects(
      {
        ...this.state.lastSearchParams,
        page: this.state.lastSearchParams.page + 1
      },
      true
    );

  handleReload = () =>
    this.fetchProjects({
      ...this.state.lastSearchParams,
      page: 1
    });

  handleSearch = (query: string, selected: Filter) =>
    this.fetchProjects({
      ...this.state.lastSearchParams,
      page: 1,
      query,
      selected
    });

  handleSelect = (key: string) =>
    associateProject(this.props.profile.key, key).then(() => {
      if (this.mounted) {
        this.setState((state: State) => ({
          listHasBeenTouched: true,
          selectedProjects: [...state.selectedProjects, key]
        }));
      }
    });

  handleUnselect = (key: string) =>
    dissociateProject(this.props.profile.key, key).then(() => {
      if (this.mounted) {
        this.setState((state: State) => ({
          listHasBeenTouched: true,
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
    return project === undefined ? key : project.name;
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
            needReload={
              this.state.listHasBeenTouched && this.state.lastSearchParams.selected !== Filter.All
            }
            onLoadMore={this.handleLoadMore}
            onReload={this.handleReload}
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
