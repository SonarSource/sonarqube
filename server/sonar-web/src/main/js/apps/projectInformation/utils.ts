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
/* eslint-disable header/header */
import {
  BITBUCKET_ALM_TAG,
  BITBUCKET_ENTERPRISE_ALM_TAG,
  GIT_ALM_TAG,
  GITHUB_ALM_TAG,
  GITHUB_ENTERPRISE_ALM_TAG,
  GITLAB_ALM_TAG,
  GITLAB_ENTERPRISE_ALM_TAG,
  SALESFORCE_ALM_TAG,
} from './constants';

export const almTagsList: string[] = [
  GITHUB_ALM_TAG,
  GITLAB_ALM_TAG,
  GIT_ALM_TAG,
  BITBUCKET_ALM_TAG,
  SALESFORCE_ALM_TAG,
  GITHUB_ENTERPRISE_ALM_TAG,
  GITLAB_ENTERPRISE_ALM_TAG,
  BITBUCKET_ENTERPRISE_ALM_TAG,
];
