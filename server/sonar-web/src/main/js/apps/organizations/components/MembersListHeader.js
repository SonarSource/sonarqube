/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
//@flow
import React from 'react';
import SearchBox from '../../../components/controls/SearchBox';
import { formatMeasure } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';

/*::
type Props = {
  handleSearch: (query?: string) => void,
  total?: number
};
*/

export default function MembersListHeader({ handleSearch, total } /*: Props */) {
  return (
    <div className="panel panel-vertical bordered-bottom spacer-bottom">
      <SearchBox
        minLength={2}
        onChange={handleSearch}
        placeholder={translate('search.search_for_users')}
      />
      {total != null && (
        <span className="pull-right little-spacer-top">
          <strong>{formatMeasure(total, 'INT')}</strong> {translate('organization.members.members')}
        </span>
      )}
    </div>
  );
}
