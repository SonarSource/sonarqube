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
import * as React from 'react';
import { SuggestionsContext } from './SuggestionsContext';

interface Props {
  suggestions: string;
}

export default function Suggestions({ suggestions }: Props) {
  return (
    <SuggestionsContext.Consumer>
      {({ addSuggestions, removeSuggestions }) => (
        <SuggestionsInner
          addSuggestions={addSuggestions}
          removeSuggestions={removeSuggestions}
          suggestions={suggestions}
        />
      )}
    </SuggestionsContext.Consumer>
  );
}

interface SuggestionsInnerProps {
  addSuggestions: (key: string) => void;
  removeSuggestions: (key: string) => void;
  suggestions: string;
}

class SuggestionsInner extends React.PureComponent<SuggestionsInnerProps> {
  componentDidMount() {
    this.props.addSuggestions(this.props.suggestions);
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.suggestions !== this.props.suggestions) {
      this.props.removeSuggestions(this.props.suggestions);
      this.props.addSuggestions(prevProps.suggestions);
    }
  }

  componentWillUnmount() {
    this.props.removeSuggestions(this.props.suggestions);
  }

  render() {
    return null;
  }
}
