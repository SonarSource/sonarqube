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
// @flow
import * as api from '../../api/organizations';
import { onFail } from '../../store/rootActions';
import * as actions from '../../store/organizations/duck';
import { addGlobalSuccessMessage } from '../../store/globalMessages/duck';
import { translate } from '../../helpers/l10n';

export const fetchOrganization = (key: string): void => (dispatch: Function): Promise<*> => {
  const onFulfilled = (organization: null | {}) => {
    if (organization) {
      dispatch(actions.receiveOrganizations([organization]));
    }
  };

  return api.getOrganization(key).then(onFulfilled, onFail(dispatch));
};

export const updateOrganization = (key: string, changes: {}): void => (dispatch: Function): Promise<*> => {
  const onFulfilled = () => {
    dispatch(actions.updateOrganization(key, changes));
    dispatch(addGlobalSuccessMessage(translate('organization.updated')));
  };

  return api.updateOrganization(key, changes).then(onFulfilled, onFail(dispatch));
};

export const deleteOrganization = (key: string): void => (dispatch: Function): Promise<*> => {
  const onFulfilled = () => {
    dispatch(actions.deleteOrganization(key));
    dispatch(addGlobalSuccessMessage(translate('organization.deleted')));
  };

  return api.deleteOrganization(key).then(onFulfilled, onFail(dispatch));
};
