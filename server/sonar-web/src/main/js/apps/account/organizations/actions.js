/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import * as api from '../../../api/organizations';
import { receiveMyOrganizations } from '../../../store/organizations/duck';
import { getValues } from '../../../api/settings';
import { receiveValues } from '../../settings/store/values/actions';

export const fetchMyOrganizations = () => dispatch => {
  return api.getMyOrganizations().then(keys => {
    if (keys.length > 0) {
      return api.getOrganizations(keys).then(({ organizations }) => {
        return dispatch(receiveMyOrganizations(organizations));
      });
    } else {
      return dispatch(receiveMyOrganizations([]));
    }
  });
};

export const fetchIfAnyoneCanCreateOrganizations = () => dispatch => {
  return getValues('sonar.organizations.anyoneCanCreate').then(values => {
    dispatch(receiveValues(values));
  });
};
