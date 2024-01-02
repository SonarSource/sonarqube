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
import classNames from 'classnames';
import * as React from 'react';
import { ClipboardButton } from '../../components/controls/clipboard';
import { isDefined } from '../../helpers/types';
import './CodeSnippet.css';

export interface CodeSnippetProps {
  isOneLine?: boolean;
  noCopy?: boolean;
  snippet: string | (string | undefined)[];
}

export default function CodeSnippet(props: CodeSnippetProps) {
  const { isOneLine, noCopy, snippet } = props;
  const snippetRef = React.useRef<HTMLPreElement>(null);

  let finalSnippet: string;
  if (Array.isArray(snippet)) {
    finalSnippet = snippet.filter((line) => isDefined(line)).join(isOneLine ? ' ' : ' \\\n  ');
  } else {
    finalSnippet = snippet;
  }

  return (
    <div className={classNames('code-snippet spacer-top spacer-bottom display-flex-row', {})}>
      {/* eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex */}
      <pre className="flex-1" ref={snippetRef} tabIndex={0}>
        {finalSnippet}
      </pre>
      {!noCopy && <ClipboardButton copyValue={finalSnippet} />}
    </div>
  );
}
