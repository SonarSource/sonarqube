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
import SelectList, {
  SelectListFilter,
  SelectListSearchParams
} from 'sonar-ui-common/components/controls/SelectList';
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  associateGateWithProject,
  dissociateGateWithProject,
  searchProjects
} from '../../../api/quality-gates';

interface Props {
  canEdit?: boolean;
  organization?: string;
  qualityGate: T.QualityGate;
}

interface State {
  needToReload: boolean;
  lastSearchParams?: SelectListSearchParams;
  projects: Array<{ id: string; key: string; name: string; selected: boolean }>;
  projectsTotalCount?: number;
  selectedProjects: string[];
}

export default class Projects extends React.PureComponent<Props, State> {
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
    searchProjects({
      gateId: this.props.qualityGate.id,
      organization: this.props.organization,
      page: searchParams.page,
      pageSize: searchParams.pageSize,
      query: searchParams.query !== '' ? searchParams.query : undefined,
      selected: searchParams.filter
    }).then(data => {
      if (this.mounted) {
        this.setState(prevState => {
          const more = searchParams.page != null && searchParams.page > 1;

          const projects = more ? [...prevState.projects, ...data.results] : data.results;
          const newSelectedProjects = data.results
            .filter(project => project.selected)
            .map(project => project.id);
          const selectedProjects = more
            ? [...prevState.selectedProjects, ...newSelectedProjects]
            : newSelectedProjects;

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

  handleSelect = (id: string) =>
    associateGateWithProject({
      gateId: this.props.qualityGate.id,
      organization: this.props.organization,
      projectId: id
    }).then(() => {
      if (this.mounted) {
        this.setState(prevState => ({
          needToReload: true,
          selectedProjects: [...prevState.selectedProjects, id]
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
        this.setState(prevState => ({
          needToReload: true,
          selectedProjects: without(prevState.selectedProjects, id)
        }));
      }
    });

  renderElement = (id: string): React.ReactNode => {
    const project = find(this.state.projects, { id });
    return (
      <div className="select-list-list-item">
        {project === undefined ? (
          id
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
    return (
      <SelectList
        elements={this.state.projects.map(project => project.id)}
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
        readOnly={!this.props.canEdit}
        renderElement={this.renderElement}
        selectedElements={this.state.selectedProjects}
        withPaging={true}
      />
    );
  }
}
