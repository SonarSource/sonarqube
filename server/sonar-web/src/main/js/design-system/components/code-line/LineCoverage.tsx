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

import { useTheme } from '@emotion/react';
import styled from '@emotion/styled';
import React, { memo } from 'react';
import tw from 'twin.macro';
import { PopupPlacement } from '../../helpers/positioning';
import { themeColor } from '../../helpers/theme';
import { Tooltip } from '../Tooltip';
import { LineMeta } from './LineStyles';

interface Props {
  coverageStatus?: 'uncovered' | 'partially-covered' | 'covered';
  lineNumber: number;
  scrollToUncoveredLine?: boolean;
  status: string | undefined;
}

function LineCoverageFunc({ lineNumber, coverageStatus, status, scrollToUncoveredLine }: Props) {
  const coverageMarker = React.useRef<HTMLTableCellElement>(null);
  React.useEffect(() => {
    if (scrollToUncoveredLine && coverageMarker.current) {
      coverageMarker.current.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
        inline: 'center',
      });
    }
  }, [scrollToUncoveredLine, coverageMarker]);

  if (!coverageStatus) {
    return <LineMeta data-line-number={lineNumber} />;
  }

  return (
    <Tooltip content={status} placement={PopupPlacement.Right}>
      <LineMeta data-line-number={lineNumber} ref={coverageMarker}>
        {coverageStatus === 'covered' && <CoveredBlock aria-label={status} />}
        {coverageStatus === 'uncovered' && <UncoveredBlock aria-label={status} />}
        {coverageStatus === 'partially-covered' && <PartiallyCoveredBlock aria-label={status} />}
      </LineMeta>
    </Tooltip>
  );
}

export const LineCoverage = memo(LineCoverageFunc);

const CoverageBlock = styled.div`
  ${tw`sw-w-1 sw-h-full`}
  ${tw`sw-ml-1/2`}

  &, & svg {
    outline: none;
  }
`;

const CoveredBlock = styled(CoverageBlock)`
  background-color: ${themeColor('codeLineCovered')};
`;

const UncoveredBlock = styled(CoverageBlock)`
  background-color: ${themeColor('codeLineUncovered')};
`;

function PartiallyCoveredBlock(htmlProps: React.HTMLAttributes<HTMLDivElement>) {
  const theme = useTheme();
  return (
    <CoverageBlock {...htmlProps}>
      <svg fill="none" viewBox="0 0 4 18" xmlns="http://www.w3.org/2000/svg">
        <rect fill={themeColor('codeLinePartiallyCoveredA')({ theme })} height="18" width="4" />
        <path
          clipRule="evenodd"
          d="M0 0L4 3V6L0 3V0ZM0 6L4 9V12L0 9V6ZM4 15L0 12V15L4 18V15Z"
          fill={themeColor('codeLinePartiallyCoveredB')({ theme })}
          fillRule="evenodd"
        />
      </svg>
    </CoverageBlock>
  );
}
