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
import * as React from 'react';
import HelpTooltip from '../../components/controls/HelpTooltip';
import SearchBox from '../../components/controls/SearchBox';
import { getAlmMembersUrl, sanitizeAlmId } from '../../helpers/almIntegrations';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { formatMeasure } from '../../helpers/measures';

export interface Props {
  currentUser: T.LoggedInUser;
  handleSearch: (query?: string) => void;
  organization: T.Organization;
  total?: number;
}

export default function MembersListHeader({
  currentUser,
  handleSearch,
  organization,
  total
}: Props) {
  return (
    <div className="panel panel-vertical bordered-bottom spacer-bottom">
      <SearchBox
        minLength={2}
        onChange={handleSearch}
        placeholder={translate('search.search_for_users')}
      />
      {total !== undefined && (
        <span className="pull-right little-spacer-top">
          <strong>{formatMeasure(total, 'INT')}</strong> {translate('organization.members.members')}
          {organization.alm && organization.alm.membersSync && (
            <HelpTooltip
              className="spacer-left"
              overlay={
                <div className="abs-width-300 markdown cut-margins">
                  <p>
                    {translate(
                      'organization.members.auto_sync_total_help',
                      sanitizeAlmId(organization.alm.key)
                    )}
                  </p>
                  {currentUser.personalOrganization !== organization.key && (
                    <>
                      <hr />
                      <p>
                        <a
                          href={getAlmMembersUrl(organization.alm.key, organization.alm.url)}
                          rel="noopener noreferrer"
                          target="_blank">
                          {translateWithParameters(
                            'organization.members.see_all_members_on_x',
                            translate(sanitizeAlmId(organization.alm.key))
                          )}
                        </a>
                      </p>
                    </>
                  )}
                </div>
              }
            />
          )}
        </span>
      )}
    </div>
  );
}
