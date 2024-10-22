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
import { renderWithContext } from '../../../helpers/testUtils';
import { Modal, PropsWithChildren, PropsWithSections } from '../Modal';

it('should render default modal with predefined content', async () => {
  setupPredefinedContent({
    body: 'Modal body',
    headerTitle: 'Hello',
    headerDescription: 'Does this look OK?',
    secondaryButtonLabel: undefined, // should use the default of 'Close'
  });

  expect(await screen.findByText('Modal body')).toBeVisible();
  expect(await screen.findByText('Hello')).toBeVisible();
  expect(await screen.findByText('Does this look OK?')).toBeVisible();
  expect(await screen.findByRole('button', { name: 'close' })).toBeVisible();
});

it('should request close when pressing esc', async () => {
  const onClose = jest.fn();
  const { user } = setupPredefinedContent({ onClose });

  await user.keyboard('{Escape}');

  expect(onClose).toHaveBeenCalled();
});

it('should render modal with loose content', async () => {
  setupLooseContent(undefined, <div>Hello</div>);

  expect(await screen.findByText('Hello')).toBeVisible();
});

it('should request close when pressing esc on loose content', async () => {
  const onClose = jest.fn();
  const { user } = setupLooseContentWithMultipleChildren({ onClose });

  await user.keyboard('{Escape}');

  expect(onClose).toHaveBeenCalled();
});

function setupPredefinedContent(props: Partial<PropsWithSections> = {}) {
  return renderWithContext(
    <Modal
      body="Body"
      headerTitle="Hello"
      onClose={jest.fn()}
      secondaryButtonLabel="Close"
      {...props}
    />,
  );
}

function setupLooseContent(props: Partial<PropsWithChildren> = {}, children = <div />) {
  return renderWithContext(
    <Modal onClose={jest.fn()} {...props}>
      {children}
    </Modal>,
  );
}

function setupLooseContentWithMultipleChildren(props: Partial<PropsWithChildren> = {}) {
  return renderWithContext(
    <Modal onClose={jest.fn()} {...props}>
      <div>Hello there!</div>
      <div>How are you?</div>
    </Modal>,
  );
}
