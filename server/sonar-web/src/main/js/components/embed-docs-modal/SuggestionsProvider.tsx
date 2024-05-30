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

import * as React from 'react';
import { DocTitle, DocTitleKey } from '../../helpers/doc-links';
import { SuggestionLink } from '../../types/types';
import { SuggestionsContext } from './SuggestionsContext';

interface State {
  suggestions: SuggestionLink[];
}

export default class SuggestionsProvider extends React.Component<React.PropsWithChildren, State> {
  keys: Array<DocTitleKey> = [];
  state: State = { suggestions: [] };

  fetchSuggestions = () => {
    let suggestions: SuggestionLink[] = [];

    this.keys.forEach((key) => {
      suggestions = [{ link: key, text: DocTitle[key] }, ...suggestions];
    });

    this.setState({ suggestions });
  };

  addSuggestions = (newKeys: DocTitleKey[]) => {
    newKeys.forEach((newKey) => {
      if (!this.keys.includes(newKey)) {
        this.keys = [...this.keys, newKey];
      }
    });

    this.fetchSuggestions();
  };

  removeSuggestions = (oldKeys: DocTitleKey[]) => {
    oldKeys.forEach((oldKey) => {
      this.keys = this.keys.filter((key) => key !== oldKey);
    });

    this.fetchSuggestions();
  };

  render() {
    return (
      <SuggestionsContext.Provider
        value={{
          addSuggestions: this.addSuggestions,
          removeSuggestions: this.removeSuggestions,
          suggestions: this.state.suggestions,
        }}
      >
        {this.props.children}
      </SuggestionsContext.Provider>
    );
  }
}
