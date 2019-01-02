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
import { IndexLink, Link } from 'react-router';
import { translate } from '../../../helpers/l10n';
import { save } from '../../../helpers/storage';
import { RawQuery } from '../../../helpers/query';
import { PROJECTS_DEFAULT_FILTER, PROJECTS_FAVORITE, PROJECTS_ALL } from '../utils';
import { isLoggedIn } from '../../../helpers/users';

interface Props {
  currentUser: T.CurrentUser;
  organization?: { key: string };
  query?: RawQuery;
}

export default class FavoriteFilter extends React.PureComponent<Props> {
  handleSaveFavorite = () => {
    if (!this.props.organization) {
      save(PROJECTS_DEFAULT_FILTER, PROJECTS_FAVORITE);
    }
  };

  handleSaveAll = () => {
    if (!this.props.organization) {
      save(PROJECTS_DEFAULT_FILTER, PROJECTS_ALL);
    }
  };

  render() {
    if (!isLoggedIn(this.props.currentUser)) {
      return null;
    }

    const pathnameForFavorite = this.props.organization
      ? `/organizations/${this.props.organization.key}/projects/favorite`
      : '/projects/favorite';

    const pathnameForAll = this.props.organization
      ? `/organizations/${this.props.organization.key}/projects`
      : '/projects';

    return (
      <header className="page-header text-center">
        <div className="button-group">
          <Link
            activeClassName="button-active"
            className="button"
            id="favorite-projects"
            onClick={this.handleSaveFavorite}
            to={{ pathname: pathnameForFavorite, query: this.props.query }}>
            {translate('my_favorites')}
          </Link>
          <IndexLink
            activeClassName="button-active"
            className="button"
            id="all-projects"
            onClick={this.handleSaveAll}
            to={{ pathname: pathnameForAll, query: this.props.query }}>
            {translate('all')}
          </IndexLink>
        </div>
      </header>
    );
  }
}
