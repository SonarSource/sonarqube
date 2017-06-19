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
//@flow
import React from 'react';
import { withRouter } from 'react-router';
import { debounce } from 'lodash';
import { getFilterUrl } from './utils';
import SearchFilter from './SearchFilter';

type Props = {|
  className?: string,
  query: { search?: string },
  router: { push: ({ pathname: string }) => void },
  isFavorite?: boolean,
  organization?: {}
|};

class SearchFilterContainer extends React.PureComponent {
  handleSearch: (userQuery?: string) => void;
  props: Props;

  constructor(props: Props) {
    super(props);
    this.handleSearch = debounce(this.handleSearch.bind(this), 250);
  }

  handleSearch(userQuery?: string) {
    const path = getFilterUrl(this.props, { search: userQuery || null });
    this.props.router.push(path);
  }

  render() {
    return (
      <SearchFilter
        className={this.props.className}
        query={this.props.query}
        handleSearch={this.handleSearch}
      />
    );
  }
}

export default withRouter(SearchFilterContainer);
