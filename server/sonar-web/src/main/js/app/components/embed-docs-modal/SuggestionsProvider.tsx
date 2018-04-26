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
import * as PropTypes from 'prop-types';
//eslint-disable-next-line import/no-extraneous-dependencies
import * as suggestionsJson from 'Docs/EmbedDocsSuggestions.json';
import { SuggestionsContext } from './SuggestionsContext';

export interface SuggestionLink {
  text: string;
  link: string;
}

interface SuggestionsJson {
  [key: string]: Array<SuggestionLink>;
}

interface Props {
  children: ({ suggestions }: { suggestions: Array<SuggestionLink> }) => React.ReactNode;
}

interface State {
  suggestions: Array<SuggestionLink>;
}

export default class SuggestionsProvider extends React.Component<Props, State> {
  keys: string[] = [];

  static childContextTypes = {
    suggestions: PropTypes.object
  };

  state = { suggestions: [] };

  getChildContext = (): { suggestions: SuggestionsContext } => {
    return {
      suggestions: {
        addSuggestions: this.addSuggestions,
        removeSuggestions: this.removeSuggestions
      }
    };
  };

  fetchSuggestions = () => {
    const jsonList = suggestionsJson as SuggestionsJson;
    let suggestions: Array<SuggestionLink> = [];
    this.keys.forEach(key => {
      if (jsonList[key]) {
        suggestions = [...jsonList[key], ...suggestions];
      }
    });
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
    return this.props.children({ suggestions: this.state.suggestions });
  }
}
