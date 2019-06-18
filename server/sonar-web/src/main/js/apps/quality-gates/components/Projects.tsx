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
  associateGateWithProject,
  dissociateGateWithProject,
  searchProjects
} from '../../../api/quality-gates';
import SelectList, { Filter } from '../../../components/SelectList/SelectList';
import { translate } from '../../../helpers/l10n';

interface Props {
  canEdit?: boolean;
  organization?: string;
  qualityGate: T.QualityGate;
}

export interface SearchParams {
  gateId: number;
  organization?: string;
  page: number;
  pageSize: number;
  query?: string;
  selected: string;
}

interface State {
  lastSearchParams: SearchParams;
  listHasBeenTouched: boolean;
  projects: Array<{ id: string; name: string; selected: boolean }>;
  projectsTotalCount?: number;
  selectedProjects: string[];
}

const PAGE_SIZE = 100;

export default class Projects extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      lastSearchParams: {
        gateId: props.qualityGate.id,
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
    searchProjects({
      ...searchParams,
      query: searchParams.query !== '' ? searchParams.query : undefined
    }).then(data => {
      if (this.mounted) {
        this.setState(prevState => {
          const projects = more ? [...prevState.projects, ...data.results] : data.results;
          const newSelectedProjects = data.results
            .filter(project => project.selected)
            .map(project => project.id);
          const selectedProjects = more
            ? [...prevState.selectedProjects, ...newSelectedProjects]
            : newSelectedProjects;

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

  handleSearch = (query: string, selected: string) =>
    this.fetchProjects({
      ...this.state.lastSearchParams,
      page: 1,
      query,
      selected
    });

  handleSelect = (id: string) =>
    associateGateWithProject({
      gateId: this.props.qualityGate.id,
      organization: this.props.organization,
      projectId: id
    }).then(() => {
      if (this.mounted) {
        this.setState(state => ({
          listHasBeenTouched: true,
          selectedProjects: [...state.selectedProjects, id]
        }));
      }
    });

  handleUnselect = (id: string) =>
    dissociateGateWithProject({
      gateId: this.props.qualityGate.id,
      organization: this.props.organization,
      projectId: id
    }).then(() => {
      if (this.mounted) {
        this.setState(state => ({
          listHasBeenTouched: true,
          selectedProjects: without(state.selectedProjects, id)
        }));
      }
    });

  renderElement = (id: string): React.ReactNode => {
    const project = find(this.state.projects, { id });
    return project === undefined ? id : project.name;
  };

  render() {
    return (
      <SelectList
        elements={this.state.projects.map(project => project.id)}
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
        readOnly={!this.props.canEdit}
        renderElement={this.renderElement}
        selectedElements={this.state.selectedProjects}
      />
    );
  }
}
