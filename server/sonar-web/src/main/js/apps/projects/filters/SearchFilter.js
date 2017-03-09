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
import { withRouter } from 'react-router';
import classNames from 'classnames';
import debounce from 'lodash/debounce';
import { getFilterUrl } from './utils';
import { translate, translateWithParameters } from '../../../helpers/l10n';

type Props = {
  query?: string,
  router: { push: (string) => void },
  isFavorite?: boolean,
  organization?: {}
};

type State = {
  userQuery?: string
};

class SearchFilter extends React.PureComponent {
  handleSearch: (string) => void;
  props: Props;
  state: State;

  constructor (props: Props) {
    super(props);
    this.state = { userQuery: props.query };
    this.handleSearch = debounce(this.handleSearch.bind(this), 250);
  }

  componentWillReceiveProps (nextProps: Props) {
    if (this.props.query === this.state.userQuery && nextProps.query !== this.props.query) {
      this.setState({ userQuery: nextProps.query || '' });
    }
  }

  handleSearch (userQuery) {
    const path = getFilterUrl(this.props, { search: userQuery || null });
    this.props.router.push(path);
  }

  handleQueryChange (userQuery) {
    this.setState({ userQuery });
    if (!userQuery || userQuery.length >= 2) {
      this.handleSearch(userQuery);
    }
  }

  render () {
    const { userQuery } = this.state;
    const inputClassName = classNames('input-super-large', {
      touched: userQuery && userQuery.length < 2
    });

    return (
      <div className="projects-facet-search" data-key="search">
        <input
          type="search"
          value={userQuery || ''}
          className={inputClassName}
          placeholder={translate('projects.search')}
          onChange={event => this.handleQueryChange(event.target.value)}
          autoComplete="off"/>
        <span className="note spacer-left">
          {translateWithParameters('select2.tooShort', 2)}
        </span>
      </div>
    );
  }
}

export default withRouter(SearchFilter);
