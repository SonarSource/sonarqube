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
import * as classNames from 'classnames';
import * as React from 'react';
import { ClipboardButton } from 'sonar-ui-common/components/controls/clipboard';
import './CodeSnippet.css';

interface Props {
  className?: string;
  isOneLine?: boolean;
  noCopy?: boolean;
  render?: () => JSX.Element;
  snippet: string | (string | undefined)[];
  wrap?: boolean;
}

// keep this "useless" concatentation for the readability reason
// eslint-disable-next-line no-useless-concat
const s = ' \\' + '\n  ';

export default function CodeSnippet({
  className,
  isOneLine,
  noCopy,
  render,
  snippet,
  wrap
}: Props) {
  const snippetArray = Array.isArray(snippet)
    ? snippet.filter(line => line !== undefined)
    : [snippet];
  const finalSnippet = isOneLine ? snippetArray.join(' ') : snippetArray.join(s);
  return (
    <div
      className={classNames(
        'code-snippet',
        { 'code-snippet-oneline': isOneLine, 'code-snippet-wrap': wrap },
        className
      )}>
      <pre>{render ? render() : finalSnippet}</pre>
      {!noCopy && <ClipboardButton copyValue={finalSnippet} />}
    </div>
  );
}
