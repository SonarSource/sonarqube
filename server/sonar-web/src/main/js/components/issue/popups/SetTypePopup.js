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
import BubblePopup from '../../../components/common/BubblePopup';
import IssueTypeIcon from '../../../components/ui/IssueTypeIcon';
import SelectList from '../../../components/common/SelectList';
import SelectListItem from '../../../components/common/SelectListItem';
/*:: import type { Issue } from '../types'; */

/*::
type Props = {
  issue: Issue,
  onSelect: string => void,
  popupPosition?: {}
};
*/

const TYPES = ['BUG', 'VULNERABILITY', 'CODE_SMELL'];

export default class SetTypePopup extends React.PureComponent {
  /*:: props: Props; */

  render() {
    return (
      <BubblePopup
        position={this.props.popupPosition}
        customClass="bubble-popup-menu bubble-popup-bottom">
        <SelectList
          items={TYPES}
          currentItem={this.props.issue.type}
          onSelect={this.props.onSelect}>
          {TYPES.map(type => (
            <SelectListItem key={type} item={type}>
              <IssueTypeIcon className="little-spacer-right" query={type} />
              {translate('issue.type', type)}
            </SelectListItem>
          ))}
        </SelectList>
      </BubblePopup>
    );
  }
}
