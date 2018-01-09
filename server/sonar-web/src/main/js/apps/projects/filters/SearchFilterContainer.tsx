/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import SearchBox from '../../../components/controls/SearchBox';
import { translate } from '../../../helpers/l10n';
import { RawQuery } from '../../../helpers/query';

interface Props {
  className?: string;
  query: { search?: string };
  onQueryChange: (change: RawQuery) => void;
  organization?: { key: string };
}

export default class SearchFilterContainer extends React.PureComponent<Props> {
  handleSearch = (userQuery?: string) => {
    this.props.onQueryChange({ search: userQuery });
  };

  render() {
    return (
      <div className="projects-topbar-item projects-topbar-item-search">
        <SearchBox
          minLength={2}
          onChange={this.handleSearch}
          placeholder={translate('projects.search')}
        />
      </div>
    );
  }
}
