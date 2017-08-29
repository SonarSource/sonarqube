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
import * as React from 'react';
import * as PropTypes from 'prop-types';
import { debounce } from 'lodash';
import { getFilterUrl } from './utils';
import SearchFilter from './SearchFilter';

interface Props {
  className?: string;
  query: { search?: string };
  isFavorite?: boolean;
  organization?: { key: string };
}

export default class SearchFilterContainer extends React.PureComponent<Props> {
  static contextTypes = {
    router: PropTypes.object.isRequired
  };

  constructor(props: Props) {
    super(props);
    this.handleSearch = debounce(this.handleSearch, 250);
  }

  handleSearch = (userQuery?: string) => {
    const path = getFilterUrl(this.props, { search: userQuery });
    this.context.router.push(path);
  };

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
