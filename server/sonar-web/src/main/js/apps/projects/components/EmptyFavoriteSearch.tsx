/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { Card, Note, StandoutLink } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import '../../../components/common/EmptySearch.css';
import { translate } from '../../../helpers/l10n';
import { queryToSearch } from '../../../helpers/urls';
import { Dict } from '../../../types/types';
import { Query } from '../query';

export default function EmptyFavoriteSearch({ query }: { query: Query }) {
  return (
    <Card aria-live="assertive">
      <Note className="sw-text-center sw-flex-column">
        <h3>{translate('no_results_search.favorites')}</h3>
        <div>
          <FormattedMessage
            defaultMessage={translate('no_results_search.favorites.2')}
            id="no_results_search.favorites.2"
            values={{
              url: (
                <StandoutLink
                  to={{
                    pathname: '/projects',
                    search: queryToSearch(query as Dict<string | undefined | number>),
                  }}
                >
                  {translate('all')}
                </StandoutLink>
              ),
            }}
          />
        </div>
      </Note>
    </Card>
  );
}
