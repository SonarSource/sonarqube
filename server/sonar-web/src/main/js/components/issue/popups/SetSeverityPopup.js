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
import { translate } from '../../../helpers/l10n';
import SelectList from '../../../components/common/SelectList';
import SelectListItem from '../../../components/common/SelectListItem';
import SeverityIcon from '../../../components/shared/SeverityIcon';
import { DropdownOverlay } from '../../controls/Dropdown';
/*:: import type { Issue } from '../types'; */

/*::
type Props = {
  issue: Issue,
  onSelect: string => void,
};
*/

const SEVERITY = ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'];

export default class SetSeverityPopup extends React.PureComponent {
  /*:: props: Props; */

  render() {
    return (
      <DropdownOverlay>
        <SelectList
          currentItem={this.props.issue.severity}
          items={SEVERITY}
          onSelect={this.props.onSelect}>
          {SEVERITY.map(severity => (
            <SelectListItem item={severity} key={severity}>
              <SeverityIcon className="little-spacer-right" severity={severity} />
              {translate('severity', severity)}
            </SelectListItem>
          ))}
        </SelectList>
      </DropdownOverlay>
    );
  }
}
