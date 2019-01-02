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
import * as classNames from 'classnames';
import { inRange } from 'lodash';
import './DuplicationsRating.css';

interface Props {
  muted?: boolean;
  size?: 'small' | 'normal' | 'big' | 'huge';
  value: number | null | undefined;
}

export default function DuplicationsRating({ muted = false, size = 'normal', value }: Props) {
  const className = classNames('duplications-rating', {
    'duplications-rating-small': size === 'small',
    'duplications-rating-big': size === 'big',
    'duplications-rating-huge': size === 'huge',
    'duplications-rating-muted': muted || value == null || isNaN(value),
    'duplications-rating-A': inRange(value || 0, 0, 3),
    'duplications-rating-B': inRange(value || 0, 3, 5),
    'duplications-rating-C': inRange(value || 0, 5, 10),
    'duplications-rating-D': inRange(value || 0, 10, 20),
    'duplications-rating-E': (value || 0) >= 20
  });

  return <div className={className} />;
}
