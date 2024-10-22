/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { ToggleButton } from '~design-system';
import { withRouter, WithRouterProps } from '~sonar-aligned/components/hoc/withRouter';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import { translate } from '../../../helpers/l10n';
import { save } from '../../../helpers/storage';
import { CurrentUser, isLoggedIn } from '../../../types/users';
import { PROJECTS_ALL, PROJECTS_DEFAULT_FILTER, PROJECTS_FAVORITE } from '../utils';

interface Props extends WithRouterProps {
  currentUser: CurrentUser;
}

export const FAVORITE_PATHNAME = '/projects/favorite';
export const ALL_PATHNAME = '/projects';

export class FavoriteFilter extends React.PureComponent<Props> {
  handleSaveFavorite = () => {
    save(PROJECTS_DEFAULT_FILTER, PROJECTS_FAVORITE);
  };

  handleSaveAll = () => {
    save(PROJECTS_DEFAULT_FILTER, PROJECTS_ALL);
  };

  onFavoriteChange = (favorite: boolean) => {
    if (favorite) {
      this.handleSaveFavorite();
      this.props.router.push({ pathname: FAVORITE_PATHNAME, query: this.props.location.query });
    } else {
      this.handleSaveAll();
      this.props.router.push({ pathname: ALL_PATHNAME, query: this.props.location.query });
    }
  };

  render() {
    const {
      location: { pathname },
    } = this.props;

    if (!isLoggedIn(this.props.currentUser)) {
      return null;
    }

    return (
      <div className="sw-mb-8">
        <ToggleButton
          options={[
            { value: true, label: translate('my_favorites') },
            { value: false, label: translate('all') },
          ]}
          onChange={this.onFavoriteChange}
          value={pathname === FAVORITE_PATHNAME}
        />
      </div>
    );
  }
}

export default withRouter(withCurrentUserContext(FavoriteFilter));
