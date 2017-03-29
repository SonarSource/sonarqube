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
import { isFavoriteSet, isAllSet } from '../utils';
import { searchProjects } from '../../../api/components';

type Props = {
  currentUser: { isLoggedIn: boolean },
  location: { query: {} },
  router: { replace: (path: string) => void }
};

type State = {
  shouldBeRedirected?: boolean
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
      this.props.router.replace('/projects/favorite');
    }
  }

  defineIfShouldBeRedirected() {
    if (Object.keys(this.props.location.query).length > 0) {
      // show ALL projects when there are some filters
      this.setState({ shouldBeRedirected: false });
    } else if (!this.props.currentUser.isLoggedIn) {
      // show ALL projects if user is anonymous
      this.setState({ shouldBeRedirected: false });
    } else if (isFavoriteSet()) {
      // show FAVORITE projects if "favorite" setting is explicitly set
      this.setState({ shouldBeRedirected: true });
    } else if (isAllSet()) {
      // show ALL projects if "all" setting is explicitly set
      this.setState({ shouldBeRedirected: false });
    } else {
      // otherwise, request favorites
      this.setState({ shouldBeRedirected: undefined });
      searchProjects({ filter: 'isFavorite', ps: 1 }).then(r => {
        // show FAVORITE projects if there are any
        this.setState({ shouldBeRedirected: r.paging.total > 0 });
      });
    }
  }

  render() {
    if (this.state.shouldBeRedirected == null || this.state.shouldBeRedirected === true) {
      return null;
    } else {
      return (
        <AllProjectsContainer
          isFavorite={false}
          location={this.props.location}
          user={this.props.currentUser}
        />
      );
    }
  }
}

const mapStateToProps = state => ({
  currentUser: getCurrentUser(state)
});

export default connect(mapStateToProps)(withRouter(DefaultPageSelector));
