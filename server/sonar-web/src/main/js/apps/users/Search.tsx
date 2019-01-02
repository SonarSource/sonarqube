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
import { Query } from './utils';
import SearchBox from '../../components/controls/SearchBox';
import { translate } from '../../helpers/l10n';

interface Props {
  query: Query;
  updateQuery: (newQuery: Partial<Query>) => void;
}

export default class Search extends React.PureComponent<Props> {
  handleSearch = (search: string) => {
    this.props.updateQuery({ search });
  };

  render() {
    const { query } = this.props;

    return (
      <div className="panel panel-vertical bordered-bottom spacer-bottom" id="users-search">
        <SearchBox
          minLength={2}
          onChange={this.handleSearch}
          placeholder={translate('search.search_by_login_or_name')}
          value={query.search}
        />
      </div>
    );
  }
}
