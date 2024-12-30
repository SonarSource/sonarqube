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

import styled from '@emotion/styled';
import { deburr } from 'lodash';
import * as React from 'react';
import Highlighter from 'react-highlight-words';
import { themeColor, themeContrast } from '../helpers/theme';

export const SearchHighlighterContext = React.createContext<string | undefined>(undefined);
SearchHighlighterContext.displayName = 'SearchHighlighterContext';

interface Props {
  children?: string;
  term?: string;
}

export function SearchHighlighter({ children = '', term }: Props) {
  const query = React.useContext(SearchHighlighterContext);

  const searchTerm = term ?? query;
  if (searchTerm) {
    return (
      <StyledHighlighter
        autoEscape
        sanitize={deburr}
        searchWords={[searchTerm]}
        textToHighlight={children}
      />
    );
  }
  return <>{children}</>;
}

const StyledHighlighter = styled(Highlighter)`
  mark {
    color: ${themeContrast('searchHighlight')};
    font-weight: inherit;
    background: ${themeColor('searchHighlight')};
  }
`;
