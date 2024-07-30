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
import classNames from 'classnames';
import hljs, { HighlightResult } from 'highlight.js';
import apex from 'highlightjs-apex';
import cobol from 'highlightjs-cobol';
import abap from 'highlightjs-sap-abap';
import tw from 'twin.macro';
import { themeColor, themeContrast } from '../helpers/theme';
import { hljsIssueIndicatorPlugin, hljsUnderlinePlugin } from '../sonar-aligned';

hljs.registerLanguage('abap', abap);
hljs.registerLanguage('apex', apex);
hljs.registerLanguage('cobol', cobol);

hljs.registerAliases('azureresourcemanager', { languageName: 'json' });
hljs.registerAliases('flex', { languageName: 'actionscript' });
hljs.registerAliases('objc', { languageName: 'objectivec' });
hljs.registerAliases('plsql', { languageName: 'pgsql' });
hljs.registerAliases('secrets', { languageName: 'markdown' });
hljs.registerAliases('web', { languageName: 'xml' });
hljs.registerAliases(['cloudformation', 'kubernetes'], { languageName: 'yaml' });

hljs.addPlugin(hljsIssueIndicatorPlugin);
hljs.addPlugin(hljsUnderlinePlugin);

interface Props {
  className?: string;
  htmlAsString: string;
  language?: string;
  wrap?: boolean | 'words';
}

const CODE_REGEXP = '<(code|pre)\\b([^>]*?)>(.+?)<\\/\\1>';
const GLOBAL_REGEXP = new RegExp(CODE_REGEXP, 'gs');
const SINGLE_REGEXP = new RegExp(CODE_REGEXP, 's');

const htmlDecode = (escapedCode: string) => {
  const doc = new DOMParser().parseFromString(escapedCode, 'text/html');

  return doc.documentElement.textContent ?? '';
};

export function CodeSyntaxHighlighter(props: Props) {
  const { className, htmlAsString, language, wrap } = props;
  let highlightedHtmlAsString = htmlAsString;

  htmlAsString.match(GLOBAL_REGEXP)?.forEach((codeBlock) => {
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    const [, tag, attributes, code] = SINGLE_REGEXP.exec(codeBlock)!;

    const unescapedCode = htmlDecode(code);

    let highlightedCode: HighlightResult;

    try {
      const actualLanguage =
        language !== undefined && hljs.getLanguage(language) ? language : 'plaintext';

      highlightedCode = hljs.highlight(unescapedCode, {
        ignoreIllegals: true,
        language: actualLanguage,
      });
    } catch {
      highlightedCode = hljs.highlight(unescapedCode, {
        ignoreIllegals: true,
        language: 'plaintext',
      });
    }

    highlightedHtmlAsString = highlightedHtmlAsString.replace(
      codeBlock,
      // Use a function to avoid triggering special replacement patterns
      // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/replace#specifying_a_string_as_the_replacement
      () => `<${tag}${attributes}>${highlightedCode.value}</${tag}>`,
    );
  });

  return (
    <StyledSpan
      className={classNames(
        `hljs ${className ?? ''}`,
        { 'code-wrap': wrap },
        { 'wrap-words': wrap === 'words' },
      )}
      // Safe: value is escaped by highlight.js
      // eslint-disable-next-line react/no-danger
      dangerouslySetInnerHTML={{ __html: highlightedHtmlAsString }}
    />
  );
}

const StyledSpan = styled.span`
  code {
    ${tw`sw-code`};

    background: ${themeColor('codeSnippetBackground')};
    color: ${themeColor('codeSnippetBody')};

    &.hljs {
      padding: unset;
    }
  }

  .hljs-meta,
  .hljs-variable {
    color: ${themeColor('codeSnippetBody')};
  }

  .hljs-doctag,
  .hljs-title,
  .hljs-title.class_,
  .hljs-title.function_ {
    color: ${themeColor('codeSnippetAnnotations')};
  }

  .hljs-comment {
    ${tw`sw-code-comment`}

    color: ${themeColor('codeSnippetComments')};
  }

  .hljs-keyword,
  .hljs-tag,
  .hljs-type {
    color: ${themeColor('codeSnippetKeyword')};
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

  .sonar-underline {
    text-decoration: underline ${themeColor('codeLineIssueSquiggle')}; // Fallback
    text-decoration: underline ${themeColor('codeLineIssueSquiggle')} wavy;
    text-decoration-thickness: 2px;
    text-decoration-skip-ink: none;
  }

  &.code-wrap {
    ${tw`sw-whitespace-pre-wrap`}
    ${tw`sw-break-all`}

    &.wrap-words {
      word-break: normal;
      ${tw`sw-break-words`}
    }
  }

  mark {
    ${tw`sw-font-regular`}
    ${tw`sw-p-1`}
    ${tw`sw-rounded-1`}

    background-color: ${themeColor('codeSnippetHighlight')};
    color: ${themeContrast('codeSnippetHighlight')};
  }
`;
