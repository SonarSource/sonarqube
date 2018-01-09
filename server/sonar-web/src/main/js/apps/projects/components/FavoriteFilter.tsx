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
import { IndexLink, Link } from 'react-router';
import { translate } from '../../../helpers/l10n';
import { CurrentUser, isLoggedIn } from '../../../app/types';
import { saveAll, saveFavorite } from '../../../helpers/storage';
import { RawQuery } from '../../../helpers/query';

interface Props {
  currentUser: CurrentUser;
  organization?: { key: string };
  query?: RawQuery;
}

export default class FavoriteFilter extends React.PureComponent<Props> {
  handleSaveFavorite = () => {
    if (!this.props.organization) {
      saveFavorite();
    }
  };

  handleSaveAll = () => {
    if (!this.props.organization) {
      saveAll();
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
            id="favorite-projects"
            to={{ pathname: pathnameForFavorite, query: this.props.query }}
            className="button"
            activeClassName="button-active"
            onClick={this.handleSaveFavorite}>
            {translate('my_favorites')}
          </Link>
          <IndexLink
            id="all-projects"
            to={{ pathname: pathnameForAll, query: this.props.query }}
            className="button"
            activeClassName="button-active"
            onClick={this.handleSaveAll}>
            {translate('all')}
          </IndexLink>
        </div>
      </header>
    );
  }
}
