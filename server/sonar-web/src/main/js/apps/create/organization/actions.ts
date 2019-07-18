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
import { Dispatch } from 'redux';
import * as api from '../../../api/organizations';
import { isGithub } from '../../../helpers/almIntegrations';
import * as actions from '../../../store/organizations';

export function createOrganization({
  alm,
  ...organization
}: T.Organization & { installationId?: string }) {
  return (dispatch: Dispatch) => {
    return api
      .createOrganization({ ...organization, name: organization.name || organization.key })
      .then((newOrganization: T.Organization) => {
        dispatch(actions.createOrganization({ ...newOrganization, alm }));
        if (alm && alm.membersSync && !alm.personal && isGithub(alm.key)) {
          api.syncMembers(newOrganization.key);
        }
        return newOrganization.key;
      });
  };
}
