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
import * as React from 'react';
import {translate} from "../../../../helpers/l10n";
import { AppState } from "../../../../types/appstate";
import withAppStateContext from "../../app-state/withAppStateContext";
import { GlobalSettingKeys } from "../../../../types/settings";
import { DropdownMenu } from "@sonarsource/echoes-react";

interface Props {
  appState: AppState;
}

function GlobalNavPlusMenu(props: Props) {

  const { appState: { settings, canAdmin, canCustomerAdmin } } = props;
  const anyoneCanCreate = settings[GlobalSettingKeys.OrganizationsAnyoneCanCreate] === 'true';
  const canCreateOrganizations = (anyoneCanCreate || canAdmin || canCustomerAdmin);

  return (
    <>
      <DropdownMenu.ItemLink to="/projects/create">
        {translate('my_account.analyze_new_project')}
      </DropdownMenu.ItemLink>
      {canCreateOrganizations && (
        <DropdownMenu.ItemLink to="/organizations/create">
          {translate('my_account.create_new_organization')}
        </DropdownMenu.ItemLink>
      )}
    </>
  );
}

export default withAppStateContext(GlobalNavPlusMenu);
