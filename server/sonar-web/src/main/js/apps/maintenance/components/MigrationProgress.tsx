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

import { FormattedMessage } from 'react-intl';
import { themeColor } from '~design-system';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';

interface Props {
  progress: {
    completedSteps: number;
    expectedFinishTimestamp: string;
    totalSteps: number;
  };
}

export function MigrationProgress({ progress }: Props) {
  const percentage = `${(progress.completedSteps / progress.totalSteps) * 100}%`;

  return (
    <>
      <MigrationProgressContainer>
        <MigrationBackgroundBar />
        <MigrationForegroundBar width={percentage} />
      </MigrationProgressContainer>
      <div className="sw-mt-2">
        <FormattedMessage
          id="maintenance.running.progress"
          values={{
            completed: progress.completedSteps,
            total: progress.totalSteps,
          }}
        />
      </div>
      <div className="sw-mt-4">
        <FormattedMessage
          id="maintenance.running.estimate"
          values={{
            date: <DateTimeFormatter date={progress.expectedFinishTimestamp} />,
          }}
        />
      </div>
    </>
  );
}

const MigrationBackgroundBar = styled.div`
  height: 0.5rem;
  background-color: ${themeColor('progressBarBackground')};
  width: 100%;
  border-radius: 0.25rem;
`;

const MigrationForegroundBar = styled.div<{ width: string }>`
  border-top-left-radius: 0.25rem;
  border-bottom-left-radius: 0.25rem;
  position: absolute;
  top: 0;
  width: ${({ width }) => width};
  height: 0.5rem;
  background-color: ${themeColor('progressBarForeground')};
`;

const MigrationProgressContainer = styled.div`
  position: relative;
`;
