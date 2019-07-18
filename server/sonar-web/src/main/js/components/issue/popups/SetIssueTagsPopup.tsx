/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { difference, without } from 'lodash';
import * as React from 'react';
import { DropdownOverlay } from 'sonar-ui-common/components/controls/Dropdown';
import { PopupPlacement } from 'sonar-ui-common/components/ui/popups';
import { searchIssueTags } from '../../../api/issues';
import TagsSelector from '../../tags/TagsSelector';

interface Props {
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
    const availableTags = difference(this.state.searchResult, this.props.selectedTags);
    return (
      <DropdownOverlay placement={PopupPlacement.BottomRight}>
        <TagsSelector
          listSize={LIST_SIZE}
          onSearch={this.onSearch}
          onSelect={this.onSelect}
          onUnselect={this.onUnselect}
          selectedTags={this.props.selectedTags}
          tags={availableTags}
        />
      </DropdownOverlay>
    );
  }
}
