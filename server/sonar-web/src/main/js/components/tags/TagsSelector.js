/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
//import classNames from 'classnames';
//import debounce from 'lodash/debounce';
//import without from 'lodash/without';
import classNames from 'classnames';
import BubblePopup from '../common/BubblePopup';
import MultiSelect from '../common/MultiSelect';
import './TagsList.css';

type Props = {
  open: boolean,
  position: {},
  tags: Array<string>,
  selectedTags: Array<string>,
  onSearch: string => void,
  onSelect: string => void,
  onUnselect: string => void,
  popupCustomClass?: string
};

export default class TagsSelector extends React.PureComponent {
  props: Props;

  render() {
    return (
      <BubblePopup
        open={this.props.open}
        position={this.props.position}
        customClass={classNames(this.props.popupCustomClass, 'bubble-popup-menu')}
      >
        <MultiSelect
          elements={this.props.tags}
          selectedElements={this.props.selectedTags}
          onSearch={this.props.onSearch}
          onSelect={this.props.onSelect}
          onUnselect={this.props.onUnselect}
        />
      </BubblePopup>
    );
  }
}
