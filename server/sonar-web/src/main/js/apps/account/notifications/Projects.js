/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import Select from 'react-select';
import { connect } from 'react-redux';
import differenceBy from 'lodash/differenceBy';
import ProjectNotifications from './ProjectNotifications';
import { translate } from '../../../helpers/l10n';
import { getComponents } from '../../../api/components';
import { getProjectsWithNotifications } from '../../../store/rootReducer';

type Props = {
  projects: Array<{
    key: string,
    name: string
  }>
};

type State = {
  addedProjects: Array<{
    key: string,
    name: string
  }>
};

class Projects extends React.Component {
  props: Props;

  state: State = {
    addedProjects: []
  };

  componentWillReceiveProps (nextProps: Props) {
    // remove all projects from `this.state.addedProjects` that already exist in `nextProps.projects`
    const nextAddedProjects = differenceBy(
        this.state.addedProjects,
        nextProps.projects,
        project => project.key
    );

    if (nextAddedProjects.length !== this.state.addedProjects) {
      this.setState({ addedProjects: nextAddedProjects });
    }
  }

  loadOptions = query => {
    // TODO filter existing out
    return getComponents({ qualifiers: 'TRK', q: query })
        .then(r => r.components)
        .then(projects => projects.map(project => ({
          value: project.key,
          label: project.name
        })))
        .then(options => ({ options }));
  };

  handleAddProject = selected => {
    const project = { key: selected.value, name: selected.label };
    this.setState({
      addedProjects: [...this.state.addedProjects, project]
    });
  };

  render () {
    const allProjects = [...this.props.projects, ...this.state.addedProjects];

    return (
        <section>
          <h2 className="spacer-bottom">
            {translate('my_profile.per_project_notifications.title')}
          </h2>

          {allProjects.length === 0 && (
              <div className="note">
                {translate('my_account.no_project_notifications')}
              </div>
          )}

          {allProjects.map(project => (
              <ProjectNotifications key={project.key} project={project}/>
          ))}

          <div className="spacer-top panel bg-muted">
            <span className="text-middle spacer-right">
              Set notifications for:
            </span>
            <Select.Async
                name="new_project"
                style={{ width: '300px' }}
                loadOptions={this.loadOptions}
                minimumInput={2}
                onChange={this.handleAddProject}
                placeholder="Search Project"
                searchPromptText="Type at least 2 characters to search"/>
          </div>
        </section>
    );
  }
}

const mapStateToProps = state => ({
  projects: getProjectsWithNotifications(state)
});

export default connect(mapStateToProps)(Projects);

export const UnconnectedProjects = Projects;
