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
import { memo, useState } from 'react';
import tw from 'twin.macro';
import { PopupPlacement, PopupZLevel } from '../../helpers/positioning';
import { themeColor } from '../../helpers/theme';
import { DropdownToggler } from '../DropdownToggler';
import { LineMeta } from './LineStyles';

interface Props {
  ariaLabel: string;
  displayOptions: boolean;
  firstLineNumber: number;
  lineNumber: number;
  popup: React.ReactNode;
}

const FILE_TOP_THRESHOLD = 10;

function LineNumberFunc({ firstLineNumber, lineNumber, popup, displayOptions, ariaLabel }: Props) {
  const [isOpen, setIsOpen] = useState<boolean>(false);

  const hasLineNumber = Boolean(lineNumber);
  const isFileTop = lineNumber - FILE_TOP_THRESHOLD < firstLineNumber;

  if (!hasLineNumber) {
    return <LineMeta className="sw-pl-2" />;
  }

  return (
    <LineMeta className="sw-pl-2" data-line-number={lineNumber}>
      {displayOptions ? (
        <DropdownToggler
          aria-labelledby={`line-number-trigger-${lineNumber}`}
          id={`line-number-dropdown-${lineNumber}`}
          onRequestClose={() => {
            setIsOpen(false);
          }}
          open={isOpen}
          overlay={popup}
          placement={isFileTop ? PopupPlacement.Bottom : PopupPlacement.Top}
          zLevel={PopupZLevel.Global}
        >
          <LineNumberStyled
            aria-controls={`line-number-dropdown-${lineNumber}`}
            aria-expanded={isOpen}
            aria-haspopup="menu"
            aria-label={ariaLabel}
            id={`line-number-trigger-${lineNumber}`}
            onClick={() => {
              setIsOpen(true);
            }}
            role="button"
            tabIndex={0}
          >
            {lineNumber}
          </LineNumberStyled>
        </DropdownToggler>
      ) : (
        lineNumber
      )}
    </LineMeta>
  );
}

export const LineNumber = memo(LineNumberFunc);

const LineNumberStyled = styled.div`
  outline: none;

  ${tw`sw-pr-2`}
  ${tw`sw-cursor-pointer`}

  &:hover {
    color: ${themeColor('codeLineMetaHover')};
  }
`;
