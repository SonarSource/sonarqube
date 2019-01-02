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
import * as theme from '../../../app/theme';

interface Props {
  className?: string;
  inheritance: T.RuleInheritance;
}

export default function RuleInheritanceIcon({ className, inheritance, ...other }: Props) {
  const fill = inheritance === 'OVERRIDES' ? theme.red : theme.baseFontColor;

  return (
    <svg
      className={className}
      height={16}
      version="1.1"
      viewBox="0 0 16 16"
      width={16}
      xmlSpace="preserve"
      xmlnsXlink="http://www.w3.org/1999/xlink"
      {...other}>
      <path
        d="M6.25 12.5a.75.75 0 1 0-1.5 0 .75.75 0 0 0 1.5 0zm0-9a.75.75 0 1 0-1.5 0 .75.75 0 0 0 1.5 0zm5 1a.75.75 0 1 0-1.5 0 .75.75 0 0 0 1.5 0zm.75 0a1.5 1.5 0 0 1-.75 1.297c-.023 2.82-2.023 3.445-3.352 3.867-1.242.39-1.648.578-1.648 1.336v.203A1.5 1.5 0 1 1 4 12.5a1.5 1.5 0 0 1 .75-1.297V4.797A1.5 1.5 0 1 1 7 3.5a1.5 1.5 0 0 1-.75 1.297V8.68c.398-.196.82-.328 1.203-.446 1.453-.46 2.281-.804 2.297-2.437A1.5 1.5 0 1 1 12 4.5z"
        style={{ fill, fillRule: 'nonzero' }}
      />
    </svg>
  );
}
