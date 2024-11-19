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

import { Link, Text, TextSize } from '@sonarsource/echoes-react';
import { FormattedMessage } from 'react-intl';
import { FishVisual } from '~design-system';
import { queryToSearchString } from '~sonar-aligned/helpers/urls';
import { translate } from '../../../helpers/l10n';
import { Dict } from '../../../types/types';
import { Query } from '../query';

export default function EmptyFavoriteSearch({ query }: { query: Query }) {
  return (
    <div className="sw-flex sw-flex-col sw-items-center sw-py-8">
      <FishVisual />
      <Text isHighlighted size={TextSize.Large} className="sw-mt-6">
        {translate('no_results_search.favorites')}
      </Text>
      <div className="sw-my-4 sw-typo-default">
        <FormattedMessage
          defaultMessage={translate('no_results_search.favorites.2')}
          id="no_results_search.favorites.2"
          values={{
            url: (
              <Link
                to={{
                  pathname: '/projects',
                  search: queryToSearchString(query as Dict<string | undefined | number>),
                }}
              >
                {translate('all')}
              </Link>
            ),
          }}
        />
      </div>
    </div>
  );
}
