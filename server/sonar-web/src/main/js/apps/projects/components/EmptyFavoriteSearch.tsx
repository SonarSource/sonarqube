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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import { translate } from 'sonar-ui-common/helpers/l10n';
import '../../../components/common/EmptySearch.css';
import { Query } from '../query';

export default function EmptyFavoriteSearch({ query }: { query: Query }) {
  return (
    <div className="empty-search">
      <h3>{translate('no_results_search.favorites')}</h3>
      <p className="big-spacer-top">
        <FormattedMessage
          defaultMessage={translate('no_results_search.favorites.2')}
          id="no_results_search.favorites.2"
          values={{
            url: <Link to={{ pathname: '/projects', query }}>{translate('all')}</Link>
          }}
        />
      </p>
    </div>
  );
}
