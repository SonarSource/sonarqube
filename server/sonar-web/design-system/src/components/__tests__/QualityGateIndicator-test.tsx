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
import { render } from '../../helpers/testUtils';
import { FCProps } from '../../types/misc';
import { QualityGateIndicator } from '../QualityGateIndicator';

const SIZE_VS_WIDTH = {
  sm: '1rem',
  md: '1.5rem',
  xl: '4rem',
};

it.each([
  ['OK', 'sm'],
  ['OK', 'md'],
  ['OK', 'xl'],
  ['ERROR', 'sm'],
  ['ERROR', 'md'],
  ['ERROR', 'xl'],
  ['NONE', 'sm'],
  ['NONE', 'md'],
  ['NONE', 'xl'],
])(
  'render the %s status and %s size correctly',
  (status: 'ERROR' | 'OK' | 'NONE' | 'NOT_COMPUTED', size: 'sm' | 'md' | 'xl') => {
    setupWithProps({ status, size });

    expect(screen.getByRole('status')).toHaveAttribute('width', SIZE_VS_WIDTH[size]);
  },
);

it('should display tooltip', () => {
  const { rerender } = setupWithProps({
    status: 'NONE',
    ariaLabel: 'label-none',
  });
  expect(screen.getByLabelText('label-none')).toBeInTheDocument();

  rerender(<QualityGateIndicator ariaLabel="label-ok" status="OK" />);
  expect(screen.getByLabelText('label-ok')).toBeInTheDocument();

  rerender(<QualityGateIndicator ariaLabel="label-error" status="ERROR" />);
  expect(screen.getByLabelText('label-error')).toBeInTheDocument();
});

function setupWithProps(props: Partial<FCProps<typeof QualityGateIndicator>> = {}) {
  return render(<QualityGateIndicator status="OK" {...props} />);
}
