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
import Icon from './Icon';
import * as theme from '../../app/theme';

interface Props {
  className?: string;
  index: number;
  size?: number;
}

const COLORS = [theme.blue, theme.darkBlue, '#24c6e0'];

export default function ChartLegendIcon({ className, index, size }: Props) {
  const fill = COLORS[index] || COLORS[0];
  return (
    <Icon className={className} size={size}>
      <path
        d="M14.325 7.143v1.714q0 0.357-0.25 0.607t-0.607 0.25h-10.857q-0.357 0-0.607-0.25t-0.25-0.607v-1.714q0-0.357 0.25-0.607t0.607-0.25h10.857q0.357 0 0.607 0.25t0.25 0.607z"
        style={{ fill }}
      />
    </Icon>
  );
}
