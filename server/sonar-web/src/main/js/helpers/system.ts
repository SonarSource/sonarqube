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
import { isAfter } from 'date-fns';
import { AppVariablesElement } from '../types/browser';
import { getEnhancedWindow } from './browser';
import { parseDate } from './dates';

export function getBaseUrl() {
  return getEnhancedWindow().baseUrl;
}

export function getSystemStatus() {
  return getEnhancedWindow().serverStatus;
}

export function getInstance() {
  return getEnhancedWindow().instance;
}

export function isOfficial() {
  return getEnhancedWindow().official;
}

export function getReactDomContainerSelector() {
  return '#content';
}

export function initAppVariables() {
  const appVariablesDiv = document.querySelector<AppVariablesElement>(
    getReactDomContainerSelector(),
  );
  if (appVariablesDiv === null) {
    throw new Error('Failed to get app variables');
  }

  getEnhancedWindow().baseUrl = appVariablesDiv.dataset.baseUrl;
  getEnhancedWindow().serverStatus = appVariablesDiv.dataset.serverStatus;
  getEnhancedWindow().instance = appVariablesDiv.dataset.instance;
  getEnhancedWindow().official = Boolean(appVariablesDiv.dataset.official);
}

export function isCurrentVersionEOLActive(installedVersionEOL: string) {
  return isAfter(parseDate(installedVersionEOL), new Date());
}
