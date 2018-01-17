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
import * as React from 'react';
import { without } from 'lodash';
import TagsSelector from '../../../components/tags/TagsSelector';
import { BubblePopupPosition } from '../../../components/common/BubblePopup';
import { searchProjectTags } from '../../../api/components';

interface Props {
  position: BubblePopupPosition;
  project: string;
  selectedTags: string[];
  setProjectTags: (tags: string[]) => void;
}

interface State {
  searchResult: string[];
}

const LIST_SIZE = 10;

export default class MetaTagsSelector extends React.PureComponent<Props, State> {
  state: State = { searchResult: [] };

  componentDidMount() {
    this.onSearch('');
  }

  onSearch = (query: string) => {
    searchProjectTags({
      q: query,
      ps: Math.min(this.props.selectedTags.length - 1 + LIST_SIZE, 100)
    }).then(result => this.setState({ searchResult: result.tags }), () => {});
  };

  onSelect = (tag: string) => {
    this.props.setProjectTags([...this.props.selectedTags, tag]);
  };

  onUnselect = (tag: string) => {
    this.props.setProjectTags(without(this.props.selectedTags, tag));
  };

  render() {
    return (
      <TagsSelector
        position={this.props.position}
        tags={this.state.searchResult}
        selectedTags={this.props.selectedTags}
        listSize={LIST_SIZE}
        onSearch={this.onSearch}
        onSelect={this.onSelect}
        onUnselect={this.onUnselect}
      />
    );
  }
}
