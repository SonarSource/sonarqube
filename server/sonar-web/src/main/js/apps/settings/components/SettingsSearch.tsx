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

import { debounce, keyBy } from 'lodash';
import lunr, { LunrIndex } from 'lunr';
import * as React from 'react';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Router } from '~sonar-aligned/types/router';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { ExtendedSettingDefinition } from '../../../types/settings';
import { Component, Dict } from '../../../types/types';
import {
  ADDITIONAL_PROJECT_SETTING_DEFINITIONS,
  ADDITIONAL_SETTING_DEFINITIONS,
} from '../constants';
import { buildSettingLink } from '../utils';
import SettingsSearchRenderer from './SettingsSearchRenderer';

interface Props {
  component?: Component;
  definitions: ExtendedSettingDefinition[];
  router: Router;
}

interface State {
  results?: ExtendedSettingDefinition[];
  searchQuery: string;
  selectedResult?: string;
  showResults: boolean;
}

const DEBOUNCE_DELAY = 250;

export class SettingsSearch extends React.Component<Props, State> {
  definitionsByKey: Dict<ExtendedSettingDefinition>;
  index: LunrIndex;
  state: State = {
    searchQuery: '',
    showResults: false,
  };

  constructor(props: Props) {
    super(props);

    this.doSearch = debounce(this.doSearch, DEBOUNCE_DELAY);
    this.handleFocus = debounce(this.handleFocus, DEBOUNCE_DELAY);

    const definitions = props.definitions.concat(
      props.component ? ADDITIONAL_PROJECT_SETTING_DEFINITIONS : ADDITIONAL_SETTING_DEFINITIONS,
    );
    this.index = this.buildSearchIndex(definitions);
    this.definitionsByKey = keyBy(definitions, 'key');
  }

  buildSearchIndex(definitions: ExtendedSettingDefinition[]) {
    return lunr(function () {
      this.ref('key');
      this.field('key');
      this.field('name');
      this.field('description');
      this.field('splitkey');

      definitions.forEach((definition) => {
        this.add({ ...definition, splitkey: definition.key.replace('.', ' ') });
      });
    });
  }

  doSearch = (query: string) => {
    const cleanQuery = query.replace(/[\^\-+:~*]/g, '');

    if (!cleanQuery) {
      this.setState({ showResults: false });
      return;
    }

    const results = this.index
      .search(
        cleanQuery
          .split(/\s+/)
          .map((s) => `${s} *${s}*`)
          .join(' '),
      )
      .map((match) => this.definitionsByKey[match.ref]);

    this.setState({ showResults: true, results, selectedResult: results[0]?.key });
  };

  hideResults = () => {
    this.setState({ showResults: false });
  };

  handleFocus = () => {
    const { searchQuery, showResults } = this.state;
    if (searchQuery && !showResults) {
      this.setState({ showResults: true });
    }
  };

  handleSearchChange = (searchQuery: string) => {
    this.setState({ searchQuery });
    this.doSearch(searchQuery);
  };

  handleMouseOverResult = (key: string) => {
    this.setState({ selectedResult: key });
  };

  handleKeyDown = (event: React.KeyboardEvent) => {
    switch (event.nativeEvent.key) {
      case KeyboardKeys.Enter:
        event.preventDefault();
        this.openSelected();
        return;
      case KeyboardKeys.UpArrow:
        event.preventDefault();
        this.selectPrevious();
        return;
      case KeyboardKeys.DownArrow:
        event.preventDefault();
        this.selectNext();
        // keep this return to prevent fall-through in case more cases will be adder later
        // eslint-disable-next-line no-useless-return
        return;
    }
  };

  selectPrevious = () => {
    const { results, selectedResult } = this.state;

    if (results && selectedResult) {
      const index = results.findIndex((r) => r.key === selectedResult);

      if (index > 0) {
        this.setState({ selectedResult: results[index - 1].key });
      }
    }
  };

  selectNext = () => {
    const { results, selectedResult } = this.state;

    if (results && selectedResult) {
      const index = results.findIndex((r) => r.key === selectedResult);

      if (index < results.length - 1) {
        this.setState({ selectedResult: results[index + 1].key });
      }
    }
  };

  openSelected = () => {
    const { router } = this.props;
    const { selectedResult } = this.state;
    if (selectedResult) {
      const definition = this.definitionsByKey[selectedResult];
      router.push(buildSettingLink(definition, this.props.component));
      this.setState({ showResults: false });
    }
  };

  render() {
    const { component } = this.props;

    return (
      <SettingsSearchRenderer
        component={component}
        onClickOutside={this.hideResults}
        onMouseOverResult={this.handleMouseOverResult}
        onSearchInputChange={this.handleSearchChange}
        onSearchInputFocus={this.handleFocus}
        onSearchInputKeyDown={this.handleKeyDown}
        {...this.state}
      />
    );
  }
}

export default withRouter(SettingsSearch);
