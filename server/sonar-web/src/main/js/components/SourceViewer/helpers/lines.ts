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

import { intersection } from 'lodash';
import { LinearIssueLocation } from '../../../types/types';

export const LINES_TO_LOAD = 1000;

export function optimizeHighlightedSymbols(
  symbolsForLine: string[] = [],
  highlightedSymbols: string[] = [],
): string[] | undefined {
  const symbols = intersection(symbolsForLine, highlightedSymbols);

  return symbols.length ? symbols : undefined;
}

export function optimizeLocationMessage(
  highlightedLocationMessage: { index: number; text: string | undefined } | undefined,
  optimizedSecondaryIssueLocations: LinearIssueLocation[],
) {
  return highlightedLocationMessage != null &&
    optimizedSecondaryIssueLocations.some(
      (location) => location.index === highlightedLocationMessage.index,
    )
    ? highlightedLocationMessage
    : undefined;
}

/**
 * Parse lineCode HTML and return text nodes content only
 */
export const getLineCodeAsPlainText = (lineCode?: string) => {
  if (!lineCode) {
    return '';
  }
  const domParser = new DOMParser();
  const domDoc = domParser.parseFromString(lineCode, 'text/html');
  const bodyElements = domDoc.getElementsByTagName('body');
  return bodyElements.length && bodyElements[0].textContent ? bodyElements[0].textContent : '';
};
