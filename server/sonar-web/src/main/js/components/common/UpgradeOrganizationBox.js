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
// @flow
import React from 'react';
import { Link } from 'react-router';
import { translate, hasMessage } from '../../helpers/l10n';
import './UpgradeOrganizationBox.css';

/*::
type Props = {
  organization: string
};
*/

export default function UpgradeOrganizationBox(props /*: Props */) {
  return (
    <div className="boxed-group boxed-group-inner upgrade-organization-box">
      <h3 className="spacer-bottom">{translate('billing.upgrade_box.header')}</h3>

      <p>{translate('billing.upgrade_box.text')}</p>

      {hasMessage('billing.upgrade_box.button') && (
        <div className="big-spacer-top">
          <Link
            className="button"
            to={{
              pathname: `organizations/${props.organization}/extension/billing/billing`,
              query: { page: 'upgrade' }
            }}>
            {translate('billing.upgrade_box.button')}
          </Link>
        </div>
      )}
    </div>
  );
}
