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
import { themeColor } from '../helpers/theme';

export const SonarCodeColorizer = styled.div`
  & pre {
    ${tw`sw-code`}

    color: ${themeColor('codeSyntaxBody')};
  }

  /* for example java annotations */
  & .a {
    color: ${themeColor('codeSyntaxAnnotations')};
  }

  /* constants */
  & .c {
    ${tw`sw-code-highlight`}

    color: ${themeColor('codeSyntaxConstants')};
  }

  /* classic comment */
  & .cd {
    ${tw`sw-code-comment`}

    color: ${themeColor('codeSyntaxComments')};
  }

  /* javadoc */
  & .j {
    ${tw`sw-code-comment`}

    color: ${themeColor('codeSyntaxComments')};
  }

  /* C++ doc */
  & .cppd {
    ${tw`sw-code-comment`}

    color: ${themeColor('codeSyntaxComments')};
  }

  /* keyword */
  & .k {
    ${tw`sw-code-highlight`}

    color: ${themeColor('codeSyntaxKeyword')};
  }

  /* string */
  & .s {
    color: ${themeColor('codeSyntaxString')};
  }

  /* keyword light */
  & .h {
    color: ${themeColor('codeSyntaxKeywordLight')};
  }

  /* preprocessing directive */
  & .p {
    color: ${themeColor('codeSyntaxPreprocessingDirective')};
  }
`;
SonarCodeColorizer.displayName = 'SonarCodeColorizer';
