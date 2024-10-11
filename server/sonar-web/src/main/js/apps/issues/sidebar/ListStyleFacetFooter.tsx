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

import { useTheme } from '@emotion/react';
import { DiscreetLink, Theme } from 'design-system';
import * as React from 'react';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { MetricType } from '~sonar-aligned/types/metrics';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export interface Props {
  nbShown: number;
  showLess?: () => void;
  showLessAriaLabel?: string;
  showMore: () => void;
  showMoreAriaLabel?: string;
  total: number;
}

export function ListStyleFacetFooter({
  nbShown,
  showLess,
  showLessAriaLabel,
  showMore,
  showMoreAriaLabel,
  total,
}: Props) {
  const theme = useTheme() as Theme;

  const hasMore = total > nbShown;
  const allShown = Boolean(total && total === nbShown);

  return (
    <div
      className="sw-mb-2 sw-mt-2 sw-text-center"
      style={{ color: 'var(--echoes-color-text-subdued)' }}
    >
      {translateWithParameters('x_show', formatMeasure(nbShown, MetricType.Integer))}

      {hasMore && (
        <DiscreetLink
          aria-label={showMoreAriaLabel}
          className="sw-ml-2"
          onClick={(e) => {
            e.preventDefault();
            showMore();
          }}
          to="#"
        >
          {translate('show_more')}
        </DiscreetLink>
      )}

      {showLess && allShown && (
        <DiscreetLink
          aria-label={showLessAriaLabel}
          className="sw-ml-2"
          onClick={(e) => {
            e.preventDefault();
            showLess();
          }}
          to="#"
        >
          {translate('show_less')}
        </DiscreetLink>
      )}
    </div>
  );
}
