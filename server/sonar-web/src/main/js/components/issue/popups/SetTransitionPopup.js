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
import BubblePopup from '../../../components/common/BubblePopup';
import SelectList from '../../../components/common/SelectList';
import SelectListItem from '../../../components/common/SelectListItem';
import { translate } from '../../../helpers/l10n';

/*::
type Props = {
  transitions: Array<string>,
  onSelect: string => void,
  popupPosition?: {}
};
*/

export default class SetTransitionPopup extends React.PureComponent {
  /*:: props: Props; */

  render() {
    const { transitions } = this.props;
    return (
      <BubblePopup
        position={this.props.popupPosition}
        customClass="bubble-popup-menu bubble-popup-bottom">
        <SelectList items={transitions} currentItem={transitions[0]} onSelect={this.props.onSelect}>
          {transitions.map(transition => {
            return (
              <SelectListItem
                key={transition}
                item={transition}
                title={translate('issue.transition', transition, 'description')}>
                {translate('issue.transition', transition)}
              </SelectListItem>
            );
          })}
        </SelectList>
      </BubblePopup>
    );
  }
}
