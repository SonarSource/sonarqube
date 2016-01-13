/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
const ONE_SECOND = 1000;
const ONE_MINUTE = 60 * ONE_SECOND;

function format(int, suffix) {
  return `${int}${suffix}`;
}

export function formatDuration(value) {
  if (!value) {
    return '';
  }
  if (value >= ONE_MINUTE) {
    let minutes = Math.round(value / ONE_MINUTE);
    return format(minutes, 'min');
  } else if (value >= ONE_SECOND) {
    let seconds = Math.round(value / ONE_SECOND);
    return format(seconds, 's');
  } else {
    return format(value, 'ms');
  }
}
