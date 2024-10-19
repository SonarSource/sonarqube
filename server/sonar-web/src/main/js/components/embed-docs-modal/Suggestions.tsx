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
import { DocSection, DocSectionKey, DocTitleKey } from '../../helpers/doc-links';
import { isDefined } from '../../helpers/types';
import { SuggestionsContext, SuggestionsContextShape } from './SuggestionsContext';

type Props =
  | {
      suggestion: DocTitleKey;
      suggestionGroup?: never;
    }
  | {
      suggestion?: never;
      suggestionGroup: DocSectionKey;
    };

export default function Suggestions({ suggestion, suggestionGroup }: Readonly<Props>) {
  return (
    <SuggestionsContext.Consumer>
      {({ addSuggestions, removeSuggestions }) => (
        <SuggestionsInner
          addSuggestions={addSuggestions}
          removeSuggestions={removeSuggestions}
          suggestion={suggestion}
          suggestionGroup={suggestionGroup}
        />
      )}
    </SuggestionsContext.Consumer>
  );
}

interface SuggestionsInnerProps {
  addSuggestions: SuggestionsContextShape['addSuggestions'];
  removeSuggestions: SuggestionsContextShape['removeSuggestions'];
  suggestion: Props['suggestion'];
  suggestionGroup: Props['suggestionGroup'];
}

class SuggestionsInner extends React.PureComponent<SuggestionsInnerProps> {
  componentDidMount() {
    this.props.addSuggestions(this.getSuggestionListFromProps());
  }

  componentWillUnmount() {
    this.props.removeSuggestions(this.getSuggestionListFromProps());
  }

  getSuggestionListFromProps() {
    const { suggestion, suggestionGroup } = this.props;

    const suggestions: DocTitleKey[] = isDefined(suggestion) ? [suggestion] : [];

    if (suggestionGroup) {
      suggestions.push(...DocSection[suggestionGroup]);
    }

    return suggestions;
  }

  render() {
    return null;
  }
}
