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
import React from 'react';
import { AppStateContext } from '../app/components/app-state/AppStateContext';

export function getUrlForDoc(url: string, version: string, to: string) {
  const isSnapshot = version.indexOf('SNAPSHOT') !== -1;
  const path = to.replace(/^\//, '');

  return isSnapshot
    ? `${url.replace(url.slice(url.lastIndexOf('/')), '/latest')}/${path}`
    : `${url}/${path}`;
}

export function useDocUrl(to: string): string;
export function useDocUrl(): (to: string) => string;
export function useDocUrl(to?: string) {
  const { version, documentationUrl } = React.useContext(AppStateContext);

  if (to) {
    return getUrlForDoc(documentationUrl, version, to);
  }

  return (to: string) => getUrlForDoc(documentationUrl, version, to);
}
