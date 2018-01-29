/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as theme from '../app/theme';

export const SEVERITIES = ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'];
export const STATUSES = ['OPEN', 'REOPENED', 'CONFIRMED', 'RESOLVED', 'CLOSED'];
export const TYPES = ['BUG', 'VULNERABILITY', 'CODE_SMELL'];
export const RULE_STATUSES = ['READY', 'BETA', 'DEPRECATED'];

export const CHART_COLORS_RANGE_PERCENT = [
  theme.green,
  theme.lightGreen,
  theme.yellow,
  theme.orange,
  theme.red
];
export const CHART_REVERSED_COLORS_RANGE_PERCENT = [
  theme.red,
  theme.orange,
  theme.yellow,
  theme.lightGreen,
  theme.green
];

export const RATING_COLORS = [theme.green, theme.lightGreen, theme.yellow, theme.orange, '#e00'];
