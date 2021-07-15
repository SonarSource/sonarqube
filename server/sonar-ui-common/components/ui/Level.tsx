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
import { formatMeasure } from '../../helpers/measures';
import './Level.css';

export interface LevelProps {
  'aria-label'?: string;
  'aria-labelledby'?: string;
  className?: string;
  level: string;
  small?: boolean;
  muted?: boolean;
}

export default function Level(props: LevelProps) {
  const formatted = formatMeasure(props.level, 'LEVEL');
  const className = classNames(props.className, 'level', 'level-' + props.level, {
    'level-small': props.small,
    'level-muted': props.muted,
  });

  return (
    <span
      aria-label={props['aria-label']}
      aria-labelledby={props['aria-labelledby']}
      className={className}>
      {formatted}
    </span>
  );
}
