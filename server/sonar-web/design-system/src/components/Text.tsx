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
import { ElementType } from 'react';
import tw from 'twin.macro';
import { themeColor, themeContrast } from '../helpers/theme';
import { SafeHTMLInjection } from '../sonar-aligned/helpers/sanitize';

interface TextBoldProps {
  className?: string;
  match?: string;
  name: string;
}

/** @deprecated Use Text (with `isHighlighted` prop) from Echoes instead.
 */
export function TextBold({ match, name, className }: TextBoldProps) {
  return match ? (
    <SafeHTMLInjection htmlAsString={match}>
      <StyledText className={className} />
    </SafeHTMLInjection>
  ) : (
    <StyledText className={className} title={name}>
      {name}
    </StyledText>
  );
}

/** @deprecated Use Text (with `isSubdued` prop) from Echoes instead.
 */
export function TextMuted({ text, className }: Readonly<{ className?: string; text: string }>) {
  return (
    <StyledMutedText className={className} title={text}>
      {text}
    </StyledMutedText>
  );
}

/** @deprecated Use Heading from Echoes instead.
 */
export function PageTitle({
  text,
  className,
  as = 'h1',
}: {
  as?: ElementType;
  className?: string;
  text: string;
}) {
  return (
    <StyledPageTitle as={as} className={className} title={text}>
      {text}
    </StyledPageTitle>
  );
}

/** @deprecated Use Text (with `colorOverride='echoes-color-text-danger'` prop) from Echoes instead.
 */
export function TextError({
  as,
  text,
  className,
}: Readonly<{
  as?: React.ElementType;
  className?: string;
  text: string | React.ReactNode;
}>) {
  if (typeof text === 'string') {
    return (
      <StyledTextError as={as} className={className} title={text}>
        {text}
      </StyledTextError>
    );
  }
  return (
    <StyledTextError as={as} className={className}>
      {text}
    </StyledTextError>
  );
}

/** @deprecated Use Text (with `colorOverride='echoes-color-text-success'` prop) from Echoes instead.
 */
export function TextSuccess({ text, className }: Readonly<{ className?: string; text: string }>) {
  return (
    <StyledTextSuccess className={className} title={text}>
      {text}
    </StyledTextSuccess>
  );
}

const StyledText = styled.span`
  ${tw`sw-inline-block`};
  ${tw`sw-truncate`};
  ${tw`sw-font-semibold`};
  ${tw`sw-max-w-abs-800`}

  mark {
    ${tw`sw-inline-block`};

    background: ${themeColor('searchHighlight')};
    color: ${themeContrast('searchHighlight')};
  }
`;

/** @deprecated Use Text (with `isSubdued` prop) from Echoes instead.
 */
export const StyledMutedText = styled(StyledText)`
  ${tw`sw-font-regular`};
  color: var(--echoes-color-text-subdued);
`;

/** @deprecated Use Heading from Echoes instead.
 */
export const StyledPageTitle = styled(StyledText)`
  ${tw`sw-block`};
  ${tw`sw-text-base`}
  color: ${themeColor('facetHeader')};
`;

const StyledTextError = styled(StyledText)`
  color: ${themeColor('danger')};
`;

const StyledTextSuccess = styled(StyledText)`
  color: ${themeColor('textSuccess')};
`;

/** @deprecated Use Text (with `isSubdued` prop) from Echoes instead.
 */
export const TextSubdued = styled.span`
  ${tw`sw-font-regular`};
  color: var(--echoes-color-text-subdued);
`;

/** @deprecated Use Text (with `isSubdued` prop) from Echoes instead.
 */
export const LightLabel = styled.span`
  color: var(--echoes-color-text-subdued);
`;

/** @deprecated Use Label or Text (with `isHighlighted` prop) from Echoes instead.
 */
export const DarkLabel = styled.label`
  color: ${themeColor('pageContentDark')};

  ${tw`sw-typo-semibold`}
`;

/** @deprecated Use Text from Echoes instead.
 */
export const LightPrimary = styled.span`
  color: ${themeContrast('primaryLight')};
`;

/** @deprecated Use Text (with `isHighlighted` prop) from Echoes instead.
 */
export const Highlight = styled.strong`
  color: ${themeColor('pageContentDark')};

  ${tw`sw-typo-semibold`}
`;
