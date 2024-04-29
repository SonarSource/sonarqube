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
import { FishVisual, Highlight, StandoutLink } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { queryToSearchString } from '~sonar-aligned/helpers/urls';
import { translate } from '../../../helpers/l10n';
import { Dict } from '../../../types/types';
import { Query } from '../query';

export default function EmptyFavoriteSearch({ query }: { query: Query }) {
  return (
    <div aria-live="assertive" className="sw-py-8 sw-text-center">
      <FishVisual />
      <Highlight as="h3" className="sw-body-md-highlight sw-mt-6">
        {translate('no_results_search.favorites')}
      </Highlight>
      <div className="sw-my-4 sw-body-sm">
        <FormattedMessage
          defaultMessage={translate('no_results_search.favorites.2')}
          id="no_results_search.favorites.2"
          values={{
            url: (
              <StandoutLink
                to={{
                  pathname: '/projects',
                  search: queryToSearchString(query as Dict<string | undefined | number>),
                }}
              >
                {translate('all')}
              </StandoutLink>
            ),
          }}
        />
      </div>
    </div>
  );
}
