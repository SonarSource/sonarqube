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
import { getFilterUrl } from './utils';
import SearchBox from '../../../components/controls/SearchBox';
import { translate } from '../../../helpers/l10n';

interface Props {
  className?: string;
  query: { search?: string };
  isFavorite?: boolean;
  organization?: string;
}

export default class SearchFilterContainer extends React.PureComponent<Props> {
  static contextTypes = {
    router: PropTypes.object.isRequired
  };

  handleSearch = (userQuery?: string) => {
    const path = getFilterUrl(this.props, { search: userQuery });
    this.context.router.push(path);
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
