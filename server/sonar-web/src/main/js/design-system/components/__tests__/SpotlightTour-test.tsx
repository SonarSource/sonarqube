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
import userEvent from '@testing-library/user-event';
import { renderWithContext } from '../../helpers/testUtils';
import { SpotlightTour, SpotlightTourProps } from '../SpotlightTour';

it('should display the spotlight tour', async () => {
  const user = userEvent.setup();
  const callback = jest.fn();
  renderSpotlightTour({ callback });

  expect(await screen.findByRole('alertdialog')).toBeInTheDocument();
  let dialog = screen.getByRole('alertdialog');
  expect(dialog).toHaveTextContent('Trust The Foo');
  expect(dialog).toHaveTextContent('Foo bar is baz');
  expect(screen.getByText('step 1 of 5')).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'next' }));

  dialog = screen.getByRole('alertdialog');
  expect(dialog).toHaveTextContent('Trust The Baz');
  expect(dialog).toHaveTextContent('Baz foo is bar');
  expect(callback).toHaveBeenCalled();

  await user.click(screen.getByRole('button', { name: 'next' }));

  dialog = screen.getByRole('alertdialog');
  expect(dialog).toHaveTextContent('Trust The Bar');
  expect(dialog).toHaveTextContent('Bar baz is foo');

  await user.click(screen.getByRole('button', { name: 'next' }));

  dialog = screen.getByRole('alertdialog');
  expect(dialog).toHaveTextContent('Trust The Foo 2');
  expect(dialog).toHaveTextContent('Foo baz is bar');

  await user.click(screen.getByRole('button', { name: 'go_back' }));

  dialog = screen.getByRole('alertdialog');
  expect(dialog).toHaveTextContent('Trust The Bar');
  expect(dialog).toHaveTextContent('Bar baz is foo');

  await user.click(screen.getByRole('button', { name: 'next' }));
  await user.click(screen.getByRole('button', { name: 'next' }));

  dialog = screen.getByRole('alertdialog');
  expect(dialog).toHaveTextContent('Trust The Baz 2');
  expect(dialog).toHaveTextContent('Baz bar is foo');

  expect(screen.queryByRole('button', { name: 'next' })).not.toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'close' }));

  expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
});

it('should not show the spotlight tour if run is false', () => {
  renderSpotlightTour({ run: false });
  expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
});

it('should allow the customization of button labels', async () => {
  const user = userEvent.setup();
  renderSpotlightTour({
    nextLabel: 'forward',
    backLabel: 'backward',
    closeLabel: 'close_me',
    skipLabel: "just don't",
    showSkipButton: true,
  });

  expect(await screen.findByRole('alertdialog')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'forward' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: "just don't" })).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'forward' }));

  expect(screen.getByRole('button', { name: 'backward' })).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'forward' }));
  await user.click(screen.getByRole('button', { name: 'forward' }));
  await user.click(screen.getByRole('button', { name: 'forward' }));

  expect(screen.getByRole('button', { name: 'close_me' })).toBeInTheDocument();
});

it('should not display steps counter when there is only one step and no render method', async () => {
  renderSpotlightTour({
    steps: [
      {
        target: '#step1',
        content: 'Foo bar is baz',
        title: 'Trust The Foo',
        placement: 'top',
      },
    ],
    stepXofYLabel: undefined,
  });

  expect(await screen.findByRole('alertdialog')).toBeInTheDocument();
  expect(screen.queryByText('step 1 of 1')).not.toBeInTheDocument();
});

function renderSpotlightTour(props: Partial<SpotlightTourProps> = {}) {
  return renderWithContext(
    <div>
      <div id="step1">This is step 1</div>
      <div id="step2">This is step 2</div>
      <div id="step3">This is step 3</div>
      <div id="step4">This is step 4</div>
      <div id="step5">This is step 5</div>

      <SpotlightTour
        continuous
        run
        stepXofYLabel={(x: number, y: number) => `step ${x} of ${y}`}
        steps={[
          {
            target: '#step1',
            content: 'Foo bar is baz',
            title: 'Trust The Foo',
            placement: 'top',
          },
          {
            target: '#step2',
            content: 'Baz foo is bar',
            title: 'Trust The Baz',
            placement: 'right',
          },
          {
            target: '#step3',
            content: 'Bar baz is foo',
            title: 'Trust The Bar',
            placement: 'bottom',
          },
          {
            target: '#step4',
            content: 'Foo baz is bar',
            title: 'Trust The Foo 2',
            placement: 'left',
          },
          {
            target: '#step5',
            content: 'Baz bar is foo',
            title: 'Trust The Baz 2',
            placement: 'center',
          },
        ]}
        {...props}
      />
    </div>,
  );
}
