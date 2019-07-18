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
import { difference, uniq, without } from 'lodash';
import * as React from 'react';
import { getRuleTags } from '../../../api/rules';
import TagsSelector from '../../../components/tags/TagsSelector';

export interface Props {
  organization: string | undefined;
  setTags: (tags: string[]) => void;
  sysTags: string[];
  tags: string[];
}

interface State {
  searchResult: string[];
}

const LIST_SIZE = 10;

export default class RuleDetailsTagsPopup extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { searchResult: [] };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  onSearch = (query: string) => {
    return getRuleTags({
      q: query,
      ps: Math.min(this.props.tags.length + LIST_SIZE, 100),
      organization: this.props.organization
    }).then(
      tags => {
        if (this.mounted) {
          // systems tags can not be unset, don't display them in the results
          this.setState({ searchResult: without(tags, ...this.props.sysTags) });
        }
      },
      () => {}
    );
  };

  onSelect = (tag: string) => {
    this.props.setTags(uniq([...this.props.tags, tag]));
  };

  onUnselect = (tag: string) => {
    this.props.setTags(without(this.props.tags, tag));
  };

  render() {
    const availableTags = difference(this.state.searchResult, this.props.tags);
    return (
      <TagsSelector
        listSize={LIST_SIZE}
        onSearch={this.onSearch}
        onSelect={this.onSelect}
        onUnselect={this.onUnselect}
        selectedTags={this.props.tags}
        tags={availableTags}
      />
    );
  }
}
