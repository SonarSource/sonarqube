/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { Link } from 'react-router';
import ChangeProjectsView from '../views/ChangeProjectsView';
import QualifierIcon from '../../../components/shared/qualifier-icon';
import { ProfileType } from '../propTypes';
import { getProfileProjects } from '../../../api/quality-profiles';
import { translate } from '../../../helpers/l10n';

export default class ProfileProjects extends React.Component {
  static propTypes = {
    profile: ProfileType,
    canAdmin: React.PropTypes.bool.isRequired
  };

  state = {
    projects: null
  };

  componentWillMount() {
    this.loadProjects = this.loadProjects.bind(this);
  }

  componentDidMount() {
    this.mounted = true;
    this.loadProjects();
  }

  componentDidUpdate(prevProps) {
    if (prevProps.profile !== this.props.profile) {
      this.loadProjects();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadProjects() {
    if (this.props.profile.isDefault) {
      return;
    }

    const data = { key: this.props.profile.key };
    getProfileProjects(data).then(r => {
      if (this.mounted) {
        this.setState({
          projects: r.results,
          more: r.more,
          loading: false
        });
      }
    });
  }

  handleChange(e) {
    e.preventDefault();
    e.target.blur();
    new ChangeProjectsView({
      profile: this.props.profile,
      loadProjects: this.props.updateProfiles
    }).render();
  }

  renderDefault() {
    return (
      <div>
        <span className="badge spacer-right">
          {translate('default')}
        </span>
        {translate('quality_profiles.projects_for_default')}
      </div>
    );
  }

  renderProjects() {
    const { projects } = this.state;

    if (projects == null) {
      return null;
    }

    if (projects.length === 0) {
      return (
        <div>
          {translate('quality_profiles.no_projects_associated_to_profile')}
        </div>
      );
    }

    return (
      <ul>
        {projects.map(project => (
          <li key={project.uuid} className="spacer-top js-profile-project" data-key={project.key}>
            <Link
              to={{ pathname: '/dashboard', query: { id: project.key } }}
              className="link-with-icon">
              <QualifierIcon qualifier="TRK" />
              {' '}
              <span>{project.name}</span>
            </Link>
          </li>
        ))}
      </ul>
    );
  }

  render() {
    return (
      <div className="quality-profile-projects">
        <header className="page-header">
          <h2 className="page-title">
            {translate('projects')}
          </h2>

          {this.props.canAdmin &&
            !this.props.profile.isDefault &&
            <div className="pull-right">
              <button className="js-change-projects" onClick={this.handleChange.bind(this)}>
                {translate('quality_profiles.change_projects')}
              </button>
            </div>}
        </header>

        {this.props.profile.isDefault ? this.renderDefault() : this.renderProjects()}
      </div>
    );
  }
}
