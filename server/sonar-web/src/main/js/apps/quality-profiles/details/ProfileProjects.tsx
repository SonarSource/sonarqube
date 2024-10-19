/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { Button, Link, Spinner } from '@sonarsource/echoes-react';
import { Badge, ContentCell, SubTitle, Table, TableRow } from 'design-system';
import * as React from 'react';
import { getProfileProjects } from '../../../api/quality-profiles';
import ListFooter from '../../../components/controls/ListFooter';
import { translate } from '../../../helpers/l10n';
import { getProjectUrl } from '../../../helpers/urls';
import { Profile } from '../types';
import ChangeProjectsForm from './ChangeProjectsForm';

interface Props {
  organization: string;
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
    total: 0,
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
    const data = { key: this.props.profile.key, p: 1 };
    getProfileProjects(data).then(({ paging, results }) => {
      if (this.mounted) {
        this.setState({
          projects: results,
          total: paging.total,
          loading: false,
          page: 1,
        });
      }
    }, this.stopLoading);
  }

  loadMore = () => {
    this.setState({ loadingMore: true });
    const data = { key: this.props.profile.key, p: this.state.page + 1 };
    getProfileProjects(data).then(({ paging, results }) => {
      if (this.mounted) {
        this.setState((state) => ({
          projects: [...state.projects, ...results],
          total: paging.total,
          loadingMore: false,
          page: state.page + 1,
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
      <>
        <Badge className="sw-mr-2">{translate('default')}</Badge>
        {translate('quality_profiles.projects_for_default')}
      </>
    );
  }

  renderProjects() {
    if (this.state.loading) {
      return <Spinner />;
    }

    const { projects } = this.state;
    const { profile } = this.props;

    if (profile.activeRuleCount === 0 && projects.length === 0) {
      return translate('quality_profiles.cannot_associate_projects_no_rules');
    }

    if (projects.length === 0) {
      return translate('quality_profiles.no_projects_associated_to_profile');
    }

    return (
      <>
        <Table columnCount={1} noSidePadding>
          {projects.map((project) => (
            <TableRow key={project.key}>
              <ContentCell>
                <Link
                  className="it__quality-profiles__project fs-mask"
                  to={getProjectUrl(project.key)}
                >
                  {project.name}
                </Link>
              </ContentCell>
            </TableRow>
          ))}
        </Table>
        {projects.length > 0 && (
          <ListFooter
            count={projects.length}
            loadMore={this.loadMore}
            loading={this.state.loadingMore}
            total={this.state.total}
          />
        )}
      </>
    );
  }

  render() {
    const { profile } = this.props;
    const hasNoActiveRules = profile.activeRuleCount === 0;
    return (
      // eslint-disable-next-line local-rules/use-metrickey-enum
      <section className="it__quality-profiles__projects" aria-label={translate('projects')}>
        <div className="sw-flex sw-items-center sw-gap-3 sw-mb-6">
          {
            // eslint-disable-next-line local-rules/use-metrickey-enum
            <SubTitle className="sw-mb-0">{translate('projects')}</SubTitle>
          }
          {profile.actions?.associateProjects && (
            <Button
              className="it__quality-profiles__change-projects"
              onClick={this.handleChangeClick}
              isDisabled={hasNoActiveRules}
            >
              {translate('quality_profiles.change_projects')}
            </Button>
          )}
        </div>

        {profile.isDefault ? this.renderDefault() : this.renderProjects()}

        {this.state.formOpen && <ChangeProjectsForm onClose={this.closeForm} profile={profile} organization={this.props.organization} />}
      </section>
    );
  }
}
