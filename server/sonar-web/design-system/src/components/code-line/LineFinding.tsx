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
import { forwardRef, Ref } from 'react';
import tw from 'twin.macro';
import { themeBorder, themeColor, themeContrast, themeShadow } from '../../helpers/theme';
import { BareButton } from '../../sonar-aligned/components/buttons';

interface Props {
  as?: React.ElementType;
  className?: string;
  issueKey: string;
  message: React.ReactNode;
  onIssueSelect?: (issueKey: string) => void;
  selected?: boolean;
}

function LineFindingFunc(
  { as, message, issueKey, selected = true, className, onIssueSelect }: Props,
  ref: Ref<HTMLButtonElement>,
) {
  return (
    <LineFindingStyled
      as={as}
      className={className}
      data-issue={issueKey}
      onClick={() => {
        if (onIssueSelect) {
          onIssueSelect(issueKey);
        }
      }}
      ref={ref}
      selected={selected}
    >
      {message}
    </LineFindingStyled>
  );
}

export const LineFinding = forwardRef<HTMLElement, Props>(LineFindingFunc);

const LineFindingStyled = styled(BareButton)<{ selected: boolean }>`
  ${tw`sw-flex sw-gap-2 sw-items-center`}
  ${tw`sw-my-3 sw-mx-1`}
  ${tw`sw-rounded-1`}
  ${tw`sw-px-3`}
  ${tw`sw-w-full`}
  ${tw`sw-box-border`}
  ${(props) => (props.selected ? tw`sw-py-3` : tw`sw-py-2`)};
  ${(props) => (props.selected ? tw`sw-body-md-highlight` : tw`sw-body-sm`)};

  border: ${(props) =>
    props.selected
      ? themeBorder('default', 'issueBoxSelectedBorder')
      : themeBorder('default', 'issueBoxBorder')};
  color: ${themeContrast('pageBlock')};
  word-break: break-word;
  background-color: ${themeColor('pageBlock')};

  :hover {
    box-shadow: ${themeShadow('sm')};
  }
`;
