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
import * as React from 'react';
import Modal from '../../../components/controls/Modal';
import SelectList from '../../../components/SelectList/SelectList';
import { translate } from '../../../helpers/l10n';
import { Profile } from '../types';
import {
  getProfileProjects,
  associateProject,
  dissociateProject
} from '../../../api/quality-profiles';

interface Props {
  onClose: () => void;
  organization: string | null;
  profile: Profile;
}

interface State {
  projects: Array<{ key: string; name: string; selected: boolean }>;
}

export default class ChangeProjectsForm extends React.PureComponent<Props> {
  container?: HTMLElement | null;
  state: State = { projects: [] };

  componentDidMount() {
    this.handleSearch('', 'selected');
  }

  handleSearch = (query: string, selected: string) => {
    const requestData: any = {
      key: this.props.profile.key,
      pageSize: 100,
      page: 1,
      selected
    };

    if (query !== '') {
      requestData.query = query;
    }

    if (this.props.organization) {
      requestData.organization = this.props.organization;
    }

    return getProfileProjects(requestData).then(
      (data: any) => {
        this.setState({
          projects: data.results.map((project: any) => {
            return { key: project.key, name: project.name, selected: project.selected };
          })
        });
      },
      () => {}
    );
  };

  handleSelect = (key: number | string) => {
    return associateProject(this.props.profile.key, String(key)).then(
      () => {
        this.setState((state: State) => {
          return {
            projects: state.projects.map((project: any) => {
              return project.key === key ? { ...project, selected: true } : project;
            })
          };
        });
      },
      () => {}
    );
  };

  handleUnselect = (key: number | string) => {
    return dissociateProject(this.props.profile.key, String(key)).then(
      () => {
        this.setState((state: State) => {
          return {
            projects: state.projects.map((project: any) => {
              return project.key === key ? { ...project, selected: false } : project;
            })
          };
        });
      },
      () => {}
    );
  };

  handleCloseClick = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  render() {
    const header = translate('projects');

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <div className="modal-head">
          <h2>{header}</h2>
        </div>

        <div className="modal-body">
          <SelectList
            elements={this.state.projects}
            labelAll={translate('quality_gates.projects.all')}
            labelDeselected={translate('quality_gates.projects.without')}
            labelSelected={translate('quality_gates.projects.with')}
            onSearch={this.handleSearch}
            onSelect={this.handleSelect}
            onUnselect={this.handleUnselect}
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
