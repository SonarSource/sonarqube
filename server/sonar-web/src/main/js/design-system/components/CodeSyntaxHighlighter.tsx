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
import { HighlightResult } from 'highlight.js';
import hljs from 'highlight.js/lib/core';
import actionscript from 'highlight.js/lib/languages/actionscript';
import bash from 'highlight.js/lib/languages/bash';
import c from 'highlight.js/lib/languages/c';
import cpp from 'highlight.js/lib/languages/cpp';
import csharp from 'highlight.js/lib/languages/csharp';
import css from 'highlight.js/lib/languages/css';
import dart from 'highlight.js/lib/languages/dart';
import docker from 'highlight.js/lib/languages/dockerfile';
import go from 'highlight.js/lib/languages/go';
import gradle from 'highlight.js/lib/languages/gradle';
import java from 'highlight.js/lib/languages/java';
import javascript from 'highlight.js/lib/languages/javascript';
import json from 'highlight.js/lib/languages/json';
import kotlin from 'highlight.js/lib/languages/kotlin';
import objectivec from 'highlight.js/lib/languages/objectivec';
import pgsql from 'highlight.js/lib/languages/pgsql';
import php from 'highlight.js/lib/languages/php';
import plaintext from 'highlight.js/lib/languages/plaintext';
import powershell from 'highlight.js/lib/languages/powershell';
import properties from 'highlight.js/lib/languages/properties';
import python from 'highlight.js/lib/languages/python';
import ruby from 'highlight.js/lib/languages/ruby';
import scala from 'highlight.js/lib/languages/scala';
import shell from 'highlight.js/lib/languages/shell';
import sql from 'highlight.js/lib/languages/sql';
import swift from 'highlight.js/lib/languages/swift';
import typescript from 'highlight.js/lib/languages/typescript';
import vbnet from 'highlight.js/lib/languages/vbnet';
import xml from 'highlight.js/lib/languages/xml';
import yaml from 'highlight.js/lib/languages/yaml';
import apex from 'highlightjs-apex';
import cobol from 'highlightjs-cobol';
import abap from 'highlightjs-sap-abap';
import tw from 'twin.macro';
import { themeColor, themeContrast } from '../helpers/theme';
import { hljsIssueIndicatorPlugin, hljsUnderlinePlugin } from '../sonar-aligned';
import { SafeHTMLInjection, SanitizeLevel } from '../sonar-aligned/helpers/sanitize';

// Supported Languages: https://highlightjs.readthedocs.io/en/latest/supported-languages.html
// Registering languages individually reduce the packaged size to ~62kb instead of ~440kb
// Terraform package is outdated an unmaintained
hljs.registerLanguage('actionscript', actionscript);
hljs.registerLanguage('bash', bash);
hljs.registerLanguage('c', c);
hljs.registerLanguage('cpp', cpp);
hljs.registerLanguage('csharp', csharp);
hljs.registerLanguage('css', css);
hljs.registerLanguage('docker', docker);
hljs.registerLanguage('go', go);
hljs.registerLanguage('gradle', gradle);
hljs.registerLanguage('java', java);
hljs.registerLanguage('javascript', javascript);
hljs.registerLanguage('json', json);
hljs.registerLanguage('dart', dart);
hljs.registerLanguage('kotlin', kotlin);
hljs.registerLanguage('objectivec', objectivec);
hljs.registerLanguage('pgsql', pgsql);
hljs.registerLanguage('php', php);
hljs.registerLanguage('plaintext', plaintext);
hljs.registerLanguage('powershell', powershell);
hljs.registerLanguage('properties', properties);
hljs.registerLanguage('python', python);
hljs.registerLanguage('ruby', ruby);
hljs.registerLanguage('scala', scala);
hljs.registerLanguage('shell', shell);
hljs.registerLanguage('sql', sql);
hljs.registerLanguage('swift', swift);
hljs.registerLanguage('typescript', typescript);
hljs.registerLanguage('vbnet', vbnet);
hljs.registerLanguage('xml', xml);
hljs.registerLanguage('yaml', yaml);
hljs.registerLanguage('abap', abap);
hljs.registerLanguage('apex', apex);
hljs.registerLanguage('cobol', cobol);

// By default, highlight js will treat unknown language as plaintext
hljs.registerAliases('azureresourcemanager', { languageName: 'json' });
hljs.registerAliases('flex', { languageName: 'actionscript' });
hljs.registerAliases('objc', { languageName: 'objectivec' });
hljs.registerAliases('plsql', { languageName: 'pgsql' });
hljs.registerAliases('ipynb', { languageName: 'python' });
// tsql plugin doesn't work with Vite and is ~13kb in size
hljs.registerAliases('tsql', { languageName: 'sql' });
hljs.registerAliases('secrets', { languageName: 'markdown' });
hljs.registerAliases('web', { languageName: 'xml' });
hljs.registerAliases(['cloudformation', 'kubernetes'], { languageName: 'yaml' });

hljs.addPlugin(hljsIssueIndicatorPlugin);
hljs.addPlugin(hljsUnderlinePlugin);

interface Props {
  className?: string;
  escapeDom?: boolean;
  htmlAsString: string;
  language?: string;
  sanitizeLevel?: SanitizeLevel;
  wrap?: boolean | 'words';
}

const CODE_REGEXP = '<(code|pre)\\b([^>]*?)>(.+?)<\\/\\1>';
const GLOBAL_REGEXP = new RegExp(CODE_REGEXP, 'gs');
const SINGLE_REGEXP = new RegExp(CODE_REGEXP, 's');

const htmlDecode = (escapedCode: string) => {
  const doc = new DOMParser().parseFromString(escapedCode, 'text/html');

  return doc.documentElement.textContent ?? '';
};

export function CodeSyntaxHighlighter(props: Readonly<Props>) {
  const {
    className,
    escapeDom = true,
    htmlAsString,
    language,
    sanitizeLevel = SanitizeLevel.FORBID_STYLE,
    wrap,
  } = props;
  let highlightedHtmlAsString = htmlAsString;

  htmlAsString.match(GLOBAL_REGEXP)?.forEach((codeBlock) => {
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    const [, tag, attributes, code] = SINGLE_REGEXP.exec(codeBlock)!;

    const unescapedCode = escapeDom ? htmlDecode(code) : code;
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
    <SafeHTMLInjection htmlAsString={highlightedHtmlAsString} sanitizeLevel={sanitizeLevel}>
      <StyledSpan
        className={classNames(
          `hljs ${className ?? ''}`,
          { 'code-wrap': wrap },
          { 'wrap-words': wrap === 'words' },
        )}
      />
    </SafeHTMLInjection>
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
