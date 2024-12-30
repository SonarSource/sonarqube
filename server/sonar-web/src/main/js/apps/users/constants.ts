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

import { translate } from '../../helpers/l10n';
import { LabelValueSelectOption } from '../../helpers/search';
import { UserActivity } from './types';

// Nb of days without connection to SQ after which a user is considered inactive:
export const USER_INACTIVITY_DAYS_THRESHOLD = 30;

export const USERS_ACTIVITY_OPTIONS: LabelValueSelectOption[] = [
  { value: UserActivity.AnyActivity, label: translate('users.activity_filter.all_users') },
  {
    value: UserActivity.ActiveSonarLintUser,
    label: translate('users.activity_filter.active_sonarlint_users'),
  },
  {
    value: UserActivity.ActiveSonarQubeUser,
    label: translate('users.activity_filter.active_sonarqube_users'),
  },
  { value: UserActivity.InactiveUser, label: translate('users.activity_filter.inactive_users') },
];
