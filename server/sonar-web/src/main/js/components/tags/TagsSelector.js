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
import BubblePopup from '../common/BubblePopup';
import MultiSelect from '../common/MultiSelect';
import './TagsList.css';

type Props = {
  position: {},
  tags: Array<string>,
  selectedTags: Array<string>,
  listSize: number,
  onSearch: (string) => void,
  onSelect: (string) => void,
  onUnselect: (string) => void
};

export default class TagsSelector extends React.PureComponent {
  validateTag: (string) => string;
  props: Props;

  validateTag(value: string) {
    // Allow only a-z, 0-9, '+', '-', '#', '.'
    return value.toLowerCase().replace(/[^a-z0-9\+\-#.]/gi, '');
  }

  render() {
    return (
      <BubblePopup
        position={this.props.position}
        customClass="bubble-popup-bottom-right bubble-popup-menu abs-width-300">
        <MultiSelect
          elements={this.props.tags}
          selectedElements={this.props.selectedTags}
          listSize={this.props.listSize}
          onSearch={this.props.onSearch}
          onSelect={this.props.onSelect}
          onUnselect={this.props.onUnselect}
          validateSearchInput={this.validateTag}
        />
      </BubblePopup>
    );
  }
}
