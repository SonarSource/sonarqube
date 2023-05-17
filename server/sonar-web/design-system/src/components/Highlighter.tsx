/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import classNames from 'classnames';
import hljs from 'highlight.js/lib/core';
import bash from 'highlight.js/lib/languages/bash';
import gradle from 'highlight.js/lib/languages/gradle';
import plaintext from 'highlight.js/lib/languages/plaintext';
import powershell from 'highlight.js/lib/languages/powershell';
import properties from 'highlight.js/lib/languages/properties';
import shell from 'highlight.js/lib/languages/shell';
import xml from 'highlight.js/lib/languages/xml';
import yaml from 'highlight.js/lib/languages/yaml';
import { useMemo } from 'react';
import tw from 'twin.macro';
import { translate } from '../helpers/l10n';
import { themeColor, themeContrast } from '../helpers/theme';
import { InteractiveIcon } from './InteractiveIcon';
import { PencilIcon } from './icons';

hljs.registerLanguage('yaml', yaml);
hljs.registerLanguage('gradle', gradle);
hljs.registerLanguage('properties', properties);
hljs.registerLanguage('xml', xml);
hljs.registerLanguage('bash', bash);
hljs.registerLanguage('powershell', powershell);
hljs.registerLanguage('shell', shell);
hljs.registerLanguage('plaintext', plaintext);

hljs.addPlugin({
  'after:highlight': (data) => {
    data.value = data.value
      .replace(/&lt;mark&gt;/g, '<mark>')
      .replace(/&lt;\/mark&gt;/g, '</mark>');
  },
});

export type RegisteredLanguages =
  | 'bash'
  | 'gradle'
  | 'plaintext'
  | 'powershell'
  | 'properties'
  | 'shell'
  | 'xml'
  | 'yaml';

interface Props {
  className?: string;
  code: string;
  highlight?: boolean;
  isSimpleOneLine?: boolean;
  language?: RegisteredLanguages;
  toggleEdit?: VoidFunction;
  wrap?: boolean;
}

export function Highlighter({
  className,
  code,
  highlight = true,
  isSimpleOneLine = false,
  language = 'yaml',
  toggleEdit,
  wrap,
}: Props) {
  const highlighted = useMemo(
    () => hljs.highlight(code, { language: highlight ? language : 'plaintext' }),
    [code, highlight, language]
  );

  return (
    <StyledPre
      className={classNames({ 'code-wrap': wrap, 'simple-one-line': isSimpleOneLine }, className)}
    >
      <code
        className={classNames('hljs', { 'sw-inline': toggleEdit })}
        // Safe: value is escaped by highlight.js
        // eslint-disable-next-line react/no-danger
        dangerouslySetInnerHTML={{ __html: highlighted.value }}
      />
      {toggleEdit && (
        <InteractiveIcon
          Icon={PencilIcon}
          aria-label={translate('edit')}
          className="sw-ml-2"
          onClick={toggleEdit}
        />
      )}
    </StyledPre>
  );
}

const StyledPre = styled.pre`
  ${tw`sw-flex sw-items-center`}
  ${tw`sw-overflow-x-auto`}
  ${tw`sw-p-6`}

  code {
    color: ${themeColor('codeSnippetBody')};
    background: ${themeColor('codeSnippetBackground')};
    ${tw`sw-code`};

    &.hljs {
      padding: unset;
    }
  }

  .hljs-variable,
  .hljs-meta {
    color: ${themeColor('codeSnippetBody')};
  }

  .hljs-doctag,
  .hljs-title,
  .hljs-title.class_,
  .hljs-title.function_ {
    color: ${themeColor('codeSnippetAnnotations')};
  }

  .hljs-comment {
    color: ${themeColor('codeSnippetComments')};

    ${tw`sw-code-comment`}
  }

  .hljs-tag,
  .hljs-type,
  .hljs-keyword {
    color: ${themeColor('codeSnippetKeyword')};

    ${tw`sw-code-highlight`}
  }

  .hljs-literal,
  .hljs-number {
    color: ${themeColor('codeSnippetConstants')};
  }

  .hljs-string {
    color: ${themeColor('codeSnippetString')};
  }

  .hljs-meta .hljs-keyword {
    color: ${themeColor('codeSnippetPreprocessingDirective')};
  }

  &.code-wrap {
    ${tw`sw-whitespace-pre-wrap`}
    ${tw`sw-break-all`}
  }

  mark {
    color: ${themeContrast('codeSnippetHighlight')};
    background-color: ${themeColor('codeSnippetHighlight')};
    ${tw`sw-font-regular`}
    ${tw`sw-rounded-1`}
    ${tw`sw-p-1`}
  }

  &.simple-one-line {
    ${tw`sw-min-h-[1.25rem]`}
    ${tw`sw-py-0 sw-px-1`}
  }
`;
