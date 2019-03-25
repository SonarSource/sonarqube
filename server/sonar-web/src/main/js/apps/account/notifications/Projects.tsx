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
import { differenceWith } from 'lodash';
import ProjectNotifications from './ProjectNotifications';
import { NotificationProject } from './types';
import { getSuggestions } from '../../../api/components';
import { AsyncSelect } from '../../../components/controls/Select';
import Organization from '../../../components/shared/Organization';
import { translate } from '../../../helpers/l10n';

export interface Props {
  addNotification: (n: T.Notification) => void;
  channels: string[];
  notificationsByProject: T.Dict<T.Notification[]>;
  projects: NotificationProject[];
  removeNotification: (n: T.Notification) => void;
  types: string[];
}

interface State {
  addedProjects: NotificationProject[];
}

export default class Projects extends React.PureComponent<Props, State> {
  state: State = { addedProjects: [] };

  componentWillReceiveProps(nextProps: Props) {
    // remove all projects from `this.state.addedProjects`
    // that already exist in `nextProps.projects`
    this.setState(state => ({
      addedProjects: differenceWith(
        state.addedProjects,
        Object.keys(nextProps.projects),
        (stateProject, propsProjectKey) => stateProject.key !== propsProjectKey
      )
    }));
  }

  loadOptions = (query: string) => {
    if (query.length < 2) {
      return Promise.resolve({ options: [] });
    }

    return getSuggestions(query)
      .then(r => {
        const projects = r.results.find(domain => domain.q === 'TRK');
        return projects ? projects.items : [];
      })
      .then(projects => {
        return projects
          .filter(
            project =>
              !this.props.projects.find(p => p.key === project.key) &&
              !this.state.addedProjects.find(p => p.key === project.key)
          )
          .map(project => ({
            value: project.key,
            label: project.name,
            organization: project.organization
          }));
      })
      .then(options => {
        return { options };
      });
  };

  handleAddProject = (selected: { label: string; organization: string; value: string }) => {
    const project = {
      key: selected.value,
      name: selected.label,
      organization: selected.organization
    };
    this.setState(state => ({
      addedProjects: [...state.addedProjects, project]
    }));
  };

  renderOption = (option: { label: string; organization: string; value: string }) => {
    return (
      <span>
        <Organization link={false} organizationKey={option.organization} />
        <strong>{option.label}</strong>
      </span>
    );
  };

  render() {
    const allProjects = [...this.props.projects, ...this.state.addedProjects];

    return (
      <section className="boxed-group">
        <h2>{translate('my_profile.per_project_notifications.title')}</h2>

        <div className="boxed-group-inner">
          {allProjects.length === 0 && (
            <div className="note">{translate('my_account.no_project_notifications')}</div>
          )}

          {allProjects.map(project => (
            <ProjectNotifications
              addNotification={this.props.addNotification}
              channels={this.props.channels}
              key={project.key}
              notifications={this.props.notificationsByProject[project.key] || []}
              project={project}
              removeNotification={this.props.removeNotification}
              types={this.props.types}
            />
          ))}

          <div className="spacer-top panel bg-muted">
            <span className="text-middle spacer-right">
              {translate('my_account.set_notifications_for')}:
            </span>
            <AsyncSelect
              autoload={false}
              cache={false}
              className="input-super-large"
              loadOptions={this.loadOptions}
              name="new_project"
              onChange={this.handleAddProject}
              optionRenderer={this.renderOption}
              placeholder={translate('my_account.search_project')}
            />
          </div>
        </div>
      </section>
    );
  }
}
