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
import { get } from 'sonar-ui-common/helpers/storage';
import { searchProjects } from '../../../api/components';
import { Location, Router, withRouter } from '../../../components/hoc/withRouter';
import { isSonarCloud } from '../../../helpers/system';
import { isLoggedIn } from '../../../helpers/users';
import { PROJECTS_ALL, PROJECTS_DEFAULT_FILTER, PROJECTS_FAVORITE } from '../utils';
import AllProjectsContainer from './AllProjectsContainer';

interface Props {
  currentUser: T.CurrentUser;
  location: Pick<Location, 'pathname' | 'query'>;
  router: Pick<Router, 'replace'>;
}

interface State {
  shouldBeRedirected?: boolean;
  shouldForceSorting?: string;
}

export class DefaultPageSelector extends React.PureComponent<Props, State> {
  state: State = {};

  componentDidMount() {
    if (isSonarCloud() && !isLoggedIn(this.props.currentUser)) {
      this.props.router.replace('/explore/projects');
    }

    if (!isSonarCloud()) {
      this.defineIfShouldBeRedirected();
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (!isSonarCloud()) {
      if (prevProps.location !== this.props.location) {
        this.defineIfShouldBeRedirected();
      } else if (this.state.shouldBeRedirected === true) {
        this.props.router.replace({ ...this.props.location, pathname: '/projects/favorite' });
      } else if (this.state.shouldForceSorting != null) {
        this.props.router.replace({
          ...this.props.location,
          query: {
            ...this.props.location.query,
            sort: this.state.shouldForceSorting
          }
        });
      }
    }
  }

  isFavoriteSet = (): boolean => {
    const setting = get(PROJECTS_DEFAULT_FILTER);
    return setting === PROJECTS_FAVORITE;
  };

  isAllSet = (): boolean => {
    const setting = get(PROJECTS_DEFAULT_FILTER);
    return setting === PROJECTS_ALL;
  };

  defineIfShouldBeRedirected() {
    if (Object.keys(this.props.location.query).length > 0) {
      // show ALL projects when there are some filters
      this.setState({ shouldBeRedirected: false, shouldForceSorting: undefined });
    } else if (!isLoggedIn(this.props.currentUser)) {
      // show ALL projects if user is anonymous
      if (!this.props.location.query || !this.props.location.query.sort) {
        // force default sorting to last analysis date
        this.setState({ shouldBeRedirected: false, shouldForceSorting: '-analysis_date' });
      } else {
        this.setState({ shouldBeRedirected: false, shouldForceSorting: undefined });
      }
    } else if (this.isFavoriteSet()) {
      // show FAVORITE projects if "favorite" setting is explicitly set
      this.setState({ shouldBeRedirected: true, shouldForceSorting: undefined });
    } else if (this.isAllSet()) {
      // show ALL projects if "all" setting is explicitly set
      this.setState({ shouldBeRedirected: false, shouldForceSorting: undefined });
    } else {
      // otherwise, request favorites
      this.setState({ shouldBeRedirected: undefined, shouldForceSorting: undefined });
      searchProjects({ filter: 'isFavorite', ps: 1 }).then(r => {
        // show FAVORITE projects if there are any
        this.setState({ shouldBeRedirected: r.paging.total > 0, shouldForceSorting: undefined });
      });
    }
  }

  render() {
    if (isSonarCloud() && isLoggedIn(this.props.currentUser)) {
      return (
        <AllProjectsContainer
          isFavorite={true}
          location={this.props.location}
          organization={undefined}
        />
      );
    }

    const { shouldBeRedirected, shouldForceSorting } = this.state;

    if (
      shouldBeRedirected !== undefined &&
      shouldBeRedirected !== true &&
      shouldForceSorting === undefined
    ) {
      return (
        <AllProjectsContainer
          isFavorite={false}
          location={this.props.location}
          organization={undefined}
        />
      );
    }

    return null;
  }
}

export default withRouter(DefaultPageSelector);
