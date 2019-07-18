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
import suggestionsJson from 'Docs/EmbedDocsSuggestions.json';
import * as React from 'react';
import { isSonarCloud } from '../../../helpers/system';
import { SuggestionsContext } from './SuggestionsContext';

type SuggestionsJson = T.Dict<T.SuggestionLink[]>;

interface State {
  suggestions: T.SuggestionLink[];
}

export default class SuggestionsProvider extends React.Component<{}, State> {
  keys: string[] = [];
  state: State = { suggestions: [] };

  fetchSuggestions = () => {
    const jsonList = suggestionsJson as SuggestionsJson;
    let suggestions: T.SuggestionLink[] = [];
    this.keys.forEach(key => {
      if (jsonList[key]) {
        suggestions = [...jsonList[key], ...suggestions];
      }
    });
    if (!isSonarCloud()) {
      suggestions = suggestions.filter(suggestion => suggestion.scope !== 'sonarcloud');
    }
    this.setState({ suggestions });
  };

  addSuggestions = (newKey: string) => {
    this.keys = [...this.keys, newKey];
    this.fetchSuggestions();
  };

  removeSuggestions = (oldKey: string) => {
    this.keys = this.keys.filter(key => key !== oldKey);
    this.fetchSuggestions();
  };

  render() {
    return (
      <SuggestionsContext.Provider
        value={{
          addSuggestions: this.addSuggestions,
          removeSuggestions: this.removeSuggestions,
          suggestions: this.state.suggestions
        }}>
        {this.props.children}
      </SuggestionsContext.Provider>
    );
  }
}
