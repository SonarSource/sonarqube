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
import { Link } from 'react-router';
import ChangeProjectsForm from './ChangeProjectsForm';
import { Profile } from '../types';
import { getProfileProjects } from '../../../api/quality-profiles';
import QualifierIcon from '../../../components/icons-components/QualifierIcon';
import { Button } from '../../../components/ui/buttons';
import ListFooter from '../../../components/controls/ListFooter';
import { translate } from '../../../helpers/l10n';

interface Props {
  organization: string | null;
  profile: Profile;
}

interface State {
  formOpen: boolean;
  loading: boolean;
  loadingMore: boolean;
  page: number;
  projects: Array<{ key: string; name: string }>;
  total: number;
}

export default class ProfileProjects extends React.PureComponent<Props, State> {
  mounted = false;

  state: State = {
    formOpen: false,
    loading: true,
    loadingMore: false,
    page: 1,
    projects: [],
    total: 0
  };

  componentDidMount() {
    this.mounted = true;
    this.loadProjects();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.profile.key !== this.props.profile.key) {
      this.loadProjects();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  loadProjects() {
    if (this.props.profile.isDefault) {
      this.setState({ loading: false, projects: [] });
      return;
    }

    this.setState({ loading: true });
    const data = { key: this.props.profile.key, page: this.state.page };
    getProfileProjects(data).then(({ paging, results }) => {
      if (this.mounted) {
        this.setState({
          projects: results,
          total: paging.total,
          loading: false
        });
      }
    }, this.stopLoading);
  }

  loadMore = () => {
    this.setState({ loadingMore: true });
    const data = { key: this.props.profile.key, page: this.state.page + 1 };
    getProfileProjects(data).then(({ paging, results }) => {
      if (this.mounted) {
        this.setState(state => ({
          projects: [...state.projects, ...results],
          total: paging.total,
          loadingMore: false
        }));
      }
    }, this.stopLoading);
  };

  handleChangeClick = () => {
    this.setState({ formOpen: true });
  };

  closeForm = () => {
    this.setState({ formOpen: false });
    this.loadProjects();
  };

  renderDefault() {
    return (
      <div>
        <span className="badge spacer-right">{translate('default')}</span>
        {translate('quality_profiles.projects_for_default')}
      </div>
    );
  }

  renderProjects() {
    if (this.state.loading) {
      return <i className="spinner" />;
    }

    const { projects } = this.state;

    if (projects.length === 0) {
      return <div>{translate('quality_profiles.no_projects_associated_to_profile')}</div>;
    }

    return (
      <>
        <ul>
          {projects.map(project => (
            <li className="spacer-top js-profile-project" data-key={project.key} key={project.key}>
              <Link
                className="link-with-icon"
                to={{ pathname: '/dashboard', query: { id: project.key } }}>
                <QualifierIcon qualifier="TRK" /> <span>{project.name}</span>
              </Link>
            </li>
          ))}
        </ul>
        <ListFooter
          count={projects.length}
          loadMore={this.loadMore}
          ready={!this.state.loadingMore}
          total={this.state.total}
        />
      </>
    );
  }

  render() {
    const { profile } = this.props;
    return (
      <div className="boxed-group quality-profile-projects">
        {profile.actions && profile.actions.associateProjects && (
          <div className="boxed-group-actions">
            <Button className="js-change-projects" onClick={this.handleChangeClick}>
              {translate('quality_profiles.change_projects')}
            </Button>
          </div>
        )}

        <header className="boxed-group-header">
          <h2>{translate('projects')}</h2>
        </header>

        <div className="boxed-group-inner">
          {profile.isDefault ? this.renderDefault() : this.renderProjects()}
        </div>

        {this.state.formOpen && (
          <ChangeProjectsForm
            onClose={this.closeForm}
            organization={this.props.organization}
            profile={profile}
          />
        )}
      </div>
    );
  }
}
