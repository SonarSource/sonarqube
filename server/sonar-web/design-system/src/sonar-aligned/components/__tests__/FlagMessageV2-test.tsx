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
import { ComponentProps } from 'react';
import { IntlShape } from 'react-intl';
import { render } from '../../../helpers/testUtils';
import { FlagMessageV2, FlagMessageV2Variant } from '../FlagMessageV2';

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

it.each([
  ['error', '1px solid rgb(249,112,102)'],
  ['warning', '1px solid rgb(248,205,92)'],
  ['success', '1px solid rgb(166,208,91)'],
  ['info', '1px solid rgb(143,202,234)'],
  ['recommended', '1px solid rgb(93,108,208)'],
])('should render properly for "%s" variant', (variant: FlagMessageV2Variant, color) => {
  renderFlagMessage({ variant });

  const item = screen.getByRole('status');
  expect(item).toBeInTheDocument();
  expect(item).toHaveStyle({ border: color });
});

it('should render correctly with optional props', async () => {
  const onDismiss = jest.fn();
  const { user } = renderFlagMessage({
    title: 'This is a title',
    hasIcon: false,
    onDismiss,
  });
  expect(screen.getByText('This is a title')).toBeInTheDocument();
  expect(screen.queryByRole('img')).not.toBeInTheDocument();
  await user.click(screen.getByRole('button'));
  expect(onDismiss).toHaveBeenCalled();
});

function renderFlagMessage(props: Partial<ComponentProps<typeof FlagMessageV2>> = {}) {
  return render(
    <FlagMessageV2 role="status" variant="error" {...props}>
      This is an error!
    </FlagMessageV2>,
  );
}
