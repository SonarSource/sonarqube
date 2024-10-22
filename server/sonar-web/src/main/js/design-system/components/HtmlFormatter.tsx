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
import tw from 'twin.macro';
import { themeBorder, themeColor } from '../helpers';

export const HtmlFormatter = styled.div`
  ${tw`sw-typo-default`}

  a {
    color: ${themeColor('linkDefault')};
    border-bottom: ${themeBorder('default', 'linkDefault')};
    ${tw`sw-no-underline sw-typo-semibold`};

    &:visited {
      color: ${themeColor('linkDefault')};
    }

    &:hover,
    &:focus,
    &:active {
      color: ${themeColor('linkActive')};
      border-bottom: ${themeBorder('default', 'linkDefault')};
    }
  }

  p,
  ul,
  ol,
  pre,
  blockquote,
  table {
    color: ${themeColor('pageContent')};
    ${tw`sw-mb-4`}
  }

  h2,
  h3 {
    color: ${themeColor('pageContentDark')};
    ${tw`sw-typo-lg-semibold`}
    ${tw`sw-my-6`}
  }

  h4,
  h5,
  h6 {
    color: ${themeColor('pageContentDark')};
    ${tw`sw-typo-semibold`}
    ${tw`sw-mt-6 sw-mb-2`}
  }

  pre,
  code {
    background-color: ${themeColor('codeSnippetBackground')};
    border: ${themeBorder('default', 'codeSnippetBorder')};
    ${tw`sw-code`}
  }

  pre {
    ${tw`sw-rounded-2`}
    ${tw`sw-relative`}
    ${tw`sw-my-2`}

    ${tw`sw-overflow-x-auto`}
    ${tw`sw-p-6`}
  }

  code {
    ${tw`sw-m-0`}
    /* 1px override is needed to prevent overlap of other code "tags" */
    ${tw`sw-py-[1px] sw-px-1`}
    ${tw`sw-rounded-1`}
    ${tw`sw-whitespace-nowrap`}
  }

  pre > code {
    ${tw`sw-p-0`}
    ${tw`sw-whitespace-pre`}
    background-color: transparent;
  }

  blockquote {
    ${tw`sw-px-4`}
    line-height: 1.5;
  }

  ul {
    ${tw`sw-pl-6`}
    ${tw`sw-flex sw-flex-col sw-gap-2`}
    list-style-type: disc;

    li::marker {
      color: ${themeColor('listMarker')};
    }
  }

  li > ul {
    ${tw`sw-my-2 sw-mx-0`}
  }

  ol {
    ${tw`sw-pl-10`};
    list-style-type: decimal;
  }

  table {
    ${tw`sw-min-w-[50%]`}
    border: ${themeBorder('default')};
    border-collapse: collapse;
  }

  th {
    ${tw`sw-py-1 sw-px-3`}
    ${tw`sw-typo-semibold`}
    ${tw`sw-text-center`}
    background-color: ${themeColor('backgroundPrimary')};
    border: ${themeBorder('default')};
  }

  td {
    ${tw`sw-py-1 sw-px-3`}
    border: ${themeBorder('default')};
  }
`;
