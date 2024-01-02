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
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../helpers/l10n';
import { Dict } from '../../../types/types';

export interface SentenceWithHighlightsProps {
  highlightKeys: string[];
  translationKey: string;
  highlightPrefixKeys?: string;
}

export default function SentenceWithHighlights({
  highlightKeys,
  translationKey,
  highlightPrefixKeys,
}: SentenceWithHighlightsProps) {
  const values: Dict<JSX.Element> = {};

  const transhighlightPrefixKeys = highlightPrefixKeys || translationKey;
  highlightKeys.forEach((key) => {
    values[key] = <strong>{translate(transhighlightPrefixKeys, 'sentence', key)}</strong>;
  });
  return (
    <FormattedMessage
      defaultMessage={translate(translationKey, 'sentence')}
      id={`${translationKey}.sentence`}
      values={values}
    />
  );
}
