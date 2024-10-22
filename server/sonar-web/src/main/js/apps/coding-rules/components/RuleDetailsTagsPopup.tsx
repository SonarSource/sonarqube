/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { MultiSelector } from '~design-system';
import { getRuleTags } from '../../../api/rules';
import { translate } from '../../../helpers/l10n';

export interface Props {
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
    }).then(
      (searchResult) => {
        if (this.mounted) {
          this.setState({ searchResult });
        }
      },
      () => {},
    );
  };

  onSelect = (tag: string) => {
    this.props.setTags(uniq([...this.props.tags, tag]));
  };

  onUnselect = (tag: string) => {
    this.props.setTags(without(this.props.tags, tag));
  };

  render() {
    const { sysTags, tags } = this.props;
    const { searchResult } = this.state;
    const availableTags = difference(searchResult, tags);
    const selectedTags = [...sysTags, ...tags];
    return (
      <MultiSelector
        createElementLabel={translate('coding_rules.create_tag')}
        headerLabel={translate('tags')}
        searchInputAriaLabel={translate('search.search_for_tags')}
        noResultsLabel={translate('no_results')}
        onSearch={this.onSearch}
        onSelect={this.onSelect}
        onUnselect={this.onUnselect}
        selectedElements={selectedTags}
        selectedElementsDisabled={sysTags}
        elements={availableTags}
        renderTooltip={(element: string, disabled: boolean) => {
          if (sysTags.includes(element) && disabled) {
            return translate('coding_rules.system_tags_tooltip');
          }
          return null;
        }}
      />
    );
  }
}
