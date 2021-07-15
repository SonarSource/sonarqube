/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { formatMeasure } from '../../helpers/measures';
import './Rating.css';

interface Props extends React.AriaAttributes {
  className?: string;
  muted?: boolean;
  small?: boolean;
  value: string | number | undefined;
}

export default function Rating({
  className,
  muted = false,
  small = false,
  value,
  ...ariaAttrs
}: Props) {
  if (value === undefined) {
    return (
      <span aria-label={translate('metric.no_rating')} {...ariaAttrs}>
        –
      </span>
    );
  }
  const formatted = formatMeasure(value, 'RATING');
  return (
    <span
      aria-label={translateWithParameters('metric.has_rating_X', formatted)}
      className={classNames(
        'rating',
        `rating-${formatted}`,
        { 'rating-small': small, 'rating-muted': muted },
        className
      )}
      {...ariaAttrs}>
      {formatted}
    </span>
  );
}
