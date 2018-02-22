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
import { BubblePopupPosition } from '../../../components/common/BubblePopup';
import TagsSelector from '../../../components/tags/TagsSelector';
import { searchIssueTags } from '../../../api/issues';

interface Props {
  popupPosition: BubblePopupPosition;
  organization: string;
  selectedTags: string[];
  setTags: (tags: string[]) => void;
}

interface State {
  searchResult: string[];
}

const LIST_SIZE = 10;

export default class SetIssueTagsPopup extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { searchResult: [] };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  onSearch = (query: string) => {
    return searchIssueTags({
      q: query,
      ps: Math.min(this.props.selectedTags.length - 1 + LIST_SIZE, 100),
      organization: this.props.organization
    }).then(
      (tags: string[]) => {
        if (this.mounted) {
          this.setState({ searchResult: tags });
        }
      },
      () => {}
    );
  };

  onSelect = (tag: string) => {
    this.props.setTags([...this.props.selectedTags, tag]);
  };

  onUnselect = (tag: string) => {
    this.props.setTags(without(this.props.selectedTags, tag));
  };

  render() {
    return (
      <TagsSelector
        listSize={LIST_SIZE}
        onSearch={this.onSearch}
        onSelect={this.onSelect}
        onUnselect={this.onUnselect}
        position={this.props.popupPosition}
        selectedTags={this.props.selectedTags}
        tags={this.state.searchResult}
      />
    );
  }
}
