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

import { screen } from '@testing-library/react';
import { IntlShape } from 'react-intl';
import { renderWithContext } from '../../helpers/testUtils';
import { FCProps } from '../../types/misc';
import { QualityGateIndicator } from '../QualityGateIndicator';

jest.mock(
  'react-intl',
  () =>
    ({
      ...jest.requireActual('react-intl'),
      useIntl: () => ({
        formatMessage: ({ id }: { id: string }, values = {}) =>
          [id, ...Object.values(values)].join('.'),
      }),
    }) as IntlShape,
);

it('should display tooltip', () => {
  const { rerender } = setupWithProps({
    size: 'sm',
    status: 'NONE',
  });
  expect(screen.getByTitle('overview.quality_gate_x.metric.level.NONE')).toBeInTheDocument();

  rerender(<QualityGateIndicator size="md" status="OK" />);
  expect(screen.getByTitle('overview.quality_gate_x.metric.level.OK')).toBeInTheDocument();

  rerender(<QualityGateIndicator size="xl" status="ERROR" />);
  expect(screen.getByTitle('overview.quality_gate_x.metric.level.ERROR')).toBeInTheDocument();
});

function setupWithProps(props: Partial<FCProps<typeof QualityGateIndicator>> = {}) {
  return renderWithContext(<QualityGateIndicator status="OK" {...props} />);
}
