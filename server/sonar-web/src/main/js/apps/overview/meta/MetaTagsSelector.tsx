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
import { differenceBy } from 'lodash';
import TagsSelector from '../../../components/tags/TagsSelector';
import { MultiSelectValue } from '../../../components/common/MultiSelect';
import { BubblePopupPosition } from '../../../components/common/BubblePopup';
import { searchProjectTags } from '../../../api/components';

interface Props {
  position: BubblePopupPosition;
  project: string;
  selectedTags: MultiSelectValue[];
  setProjectTags: (tags: MultiSelectValue[]) => void;
}

interface State {
  searchResult: MultiSelectValue[];
}

const LIST_SIZE = 10;

export default class MetaTagsSelector extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { searchResult: [] };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  onSearch = (query: string) => {
    return searchProjectTags({
      q: query,
      ps: Math.min(this.props.selectedTags.length - 1 + LIST_SIZE, 100)
    }).then(
      ({ tags }) => {
        if (this.mounted) {
          this.setState({
            searchResult: tags.map((tag: string) => {
              return { key: tag, label: tag };
            })
          });
        }
      },
      () => {}
    );
  };

  onSelect = (tag: MultiSelectValue) => {
    this.props.setProjectTags([...this.props.selectedTags, tag]);
  };

  onUnselect = (tag: MultiSelectValue) => {
    this.props.setProjectTags(this.props.selectedTags.filter(selected => selected.key !== tag.key));
  };

  render() {
    const availableTags = differenceBy(this.state.searchResult, this.props.selectedTags, 'key');
    return (
      <TagsSelector
        listSize={LIST_SIZE}
        onSearch={this.onSearch}
        onSelect={this.onSelect}
        onUnselect={this.onUnselect}
        position={this.props.position}
        selectedTags={this.props.selectedTags}
        tags={availableTags}
      />
    );
  }
}
