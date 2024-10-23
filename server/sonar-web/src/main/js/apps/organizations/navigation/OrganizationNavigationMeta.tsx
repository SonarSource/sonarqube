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
import HomePageSelect from '../../../components/controls/HomePageSelect';
import { translate } from "../../../helpers/l10n";
import { Organization } from "../../../types/types";

interface Props {
  organization: Organization;
}

export default function OrganizationNavigationMeta({ organization }: Props) {

  let orgType: any = "ORGANIZATION";
  if (window.location.href.indexOf("policy-results") > 0) {
    orgType = "POLICY_RESULTS";
  }

  return (
    <div className="sw-text-right">
      {organization.url && (
        <a
          className="spacer-right text-limited"
          href={organization.url}
          rel="nofollow"
          title={organization.url}
        >
          {organization.url}
        </a>
      )}
      <div className="text-muted">
        <strong>{translate('organization.key')}:</strong> {organization.kee}
      </div>
      <HomePageSelect currentPage={{ type: orgType, organization: organization.kee }}/>
    </div>
  );
}
