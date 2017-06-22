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
// @flow
import React from 'react';
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import AllProjectsContainer from './AllProjectsContainer';
import { getCurrentUser } from '../../../store/rootReducer';
import { isFavoriteSet, isAllSet } from '../../../helpers/storage';
import { searchProjects } from '../../../api/components';
import type { RawQuery } from '../../../helpers/query';

type Props = {
  currentUser: { isLoggedIn: boolean },
  location: { query: {} },
  router: {
    replace: (location: { pathname?: string, query?: RawQuery }) => void
  }
};

type State = {
  shouldBeRedirected?: boolean,
  shouldForceSorting?: string
};

class DefaultPageSelector extends React.PureComponent {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {};
  }

  componentDidMount() {
    this.defineIfShouldBeRedirected();
  }

  componentDidUpdate(prevProps: Props) {
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

  defineIfShouldBeRedirected() {
    if (Object.keys(this.props.location.query).length > 0) {
      // show ALL projects when there are some filters
      this.setState({ shouldBeRedirected: false, shouldForceSorting: undefined });
    } else if (!this.props.currentUser.isLoggedIn) {
      // show ALL projects if user is anonymous
      if (!this.props.location.query || !this.props.location.query.sort) {
        // force default sorting to last analysis date
        this.setState({ shouldBeRedirected: false, shouldForceSorting: '-analysis_date' });
      } else {
        this.setState({ shouldBeRedirected: false, shouldForceSorting: undefined });
      }
    } else if (isFavoriteSet()) {
      // show FAVORITE projects if "favorite" setting is explicitly set
      this.setState({ shouldBeRedirected: true, shouldForceSorting: undefined });
    } else if (isAllSet()) {
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
    const { shouldBeRedirected, shouldForceSorting } = this.state;
    if (shouldBeRedirected == null || shouldBeRedirected === true || shouldForceSorting != null) {
      return null;
    } else {
      return (
        <AllProjectsContainer
          isFavorite={false}
          location={this.props.location}
          currentUser={this.props.currentUser}
        />
      );
    }
  }
}

const mapStateToProps = state => ({
  currentUser: getCurrentUser(state)
});

export default connect(mapStateToProps)(withRouter(DefaultPageSelector));
