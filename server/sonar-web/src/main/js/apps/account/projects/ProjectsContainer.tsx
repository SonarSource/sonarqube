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
import Helmet from 'react-helmet';
import Projects from './Projects';
import { getMyProjects } from '../../../api/components';
import { translate } from '../../../helpers/l10n';

interface State {
  loading: boolean;
  page: number;
  projects?: T.MyProject[];
  total?: number;
}

export default class ProjectsContainer extends React.PureComponent<{}, State> {
  mounted = false;
  state: State = { loading: true, page: 1 };

  componentDidMount() {
    this.mounted = true;
    this.loadProjects();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadProjects(page = this.state.page) {
    this.setState({ loading: true });
    const data = { p: page, ps: 100 };
    return getMyProjects(data).then(({ paging, projects }) => {
      this.setState(state => ({
        projects: page > 1 ? [...(state.projects || []), ...projects] : projects,
        loading: false,
        page: paging.pageIndex,
        total: paging.total
      }));
    });
  }

  loadMore = () => {
    this.loadProjects(this.state.page + 1);
  };

  render() {
    const helmet = <Helmet title={translate('my_account.projects')} />;

    if (this.state.projects == null) {
      return (
        <div className="text-center">
          {helmet}
          <i className="spinner spinner-margin" />
        </div>
      );
    }

    return (
      <div className="account-body account-container">
        {helmet}
        <Projects
          loadMore={this.loadMore}
          loading={this.state.loading}
          projects={this.state.projects}
          total={this.state.total}
        />
      </div>
    );
  }
}
