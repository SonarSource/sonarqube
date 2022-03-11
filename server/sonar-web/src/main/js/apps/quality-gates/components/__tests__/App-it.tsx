/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QualityGatesServiceMock } from '../../../../api/mocks/QualityGatesServiceMock';
import { mockAppState } from '../../../../helpers/testMocks';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import { AppState } from '../../../../types/appstate';
import routes from '../../routes';

jest.mock('../../../../api/quality-gates');

let handler: QualityGatesServiceMock;

beforeAll(() => {
  handler = new QualityGatesServiceMock();
});

afterEach(() => handler.reset());

jest.setTimeout(10_000);

it('should open the default quality gates', async () => {
  renderQualityGateApp();

  expect(await screen.findAllByRole('menuitem')).toHaveLength(handler.list.length);

  const defaultQualityGate = handler.getDefaultQualityGate();
  expect(await screen.findAllByText(defaultQualityGate.name)).toHaveLength(2);
});

it('should list all quality gates', async () => {
  renderQualityGateApp();

  expect(
    await screen.findByRole('menuitem', {
      name: `${handler.getDefaultQualityGate().name} default`
    })
  ).toBeInTheDocument();
  expect(
    await screen.findByRole('menuitem', {
      name: `${handler.getBuiltInQualityGate().name} quality_gates.built_in`
    })
  ).toBeInTheDocument();
});

it('should be able to create a quality gate then delete it', async () => {
  const user = userEvent.setup();
  handler.setIsAdmin(true);
  renderQualityGateApp();

  let createButton = await screen.findByRole('button', { name: 'create' });

  // Using keyboard
  await user.click(createButton);
  let nameInput = screen.getByRole('textbox', { name: /name.*/ });
  expect(nameInput).toBeInTheDocument();
  await user.click(nameInput);
  await user.keyboard('testone{Enter}');
  expect(await screen.findByRole('menuitem', { name: 'testone' })).toBeInTheDocument();

  // Using modal button
  createButton = await screen.findByRole('button', { name: 'create' });
  await user.click(createButton);
  nameInput = screen.getByRole('textbox', { name: /name.*/ });
  const saveButton = screen.getByRole('button', { name: 'save' });

  expect(saveButton).toBeDisabled();
  await user.click(nameInput);
  await user.keyboard('testtwo');
  await user.click(saveButton);

  const newQG = await screen.findByRole('menuitem', { name: 'testtwo' });
  expect(newQG).toBeInTheDocument();

  // Delete the quality gate
  await user.click(newQG);
  const deleteButton = await screen.findByRole('button', { name: 'delete' });
  await user.click(deleteButton);
  const popup = screen.getByRole('dialog');
  const dialogDeleteButton = within(popup).getByRole('button', { name: 'delete' });
  await user.click(dialogDeleteButton);

  await waitFor(() => {
    expect(screen.queryByRole('menuitem', { name: 'testtwo' })).not.toBeInTheDocument();
  });
});

it('should be able to copy a quality gate', async () => {
  const user = userEvent.setup();
  handler.setIsAdmin(true);
  renderQualityGateApp();

  const copyButton = await screen.findByRole('button', { name: 'copy' });

  await user.click(copyButton);
  const nameInput = screen.getByRole('textbox', { name: /name.*/ });
  expect(nameInput).toBeInTheDocument();
  await user.click(nameInput);
  await user.keyboard(' bis{Enter}');

  expect(await screen.findByRole('menuitem', { name: /.* bis/ })).toBeInTheDocument();
});

it('should be able to rename a quality gate', async () => {
  const user = userEvent.setup();
  handler.setIsAdmin(true);
  renderQualityGateApp();

  const renameButton = await screen.findByRole('button', { name: 'rename' });

  await user.click(renameButton);
  const nameInput = screen.getByRole('textbox', { name: /name.*/ });
  expect(nameInput).toBeInTheDocument();
  await user.click(nameInput);
  await user.keyboard('{Control>}a{/Control}New Name{Enter}');

  expect(await screen.findByRole('menuitem', { name: /New Name.*/ })).toBeInTheDocument();
});

it('should be able to add a condition', async () => {
  const user = userEvent.setup();
  handler.setIsAdmin(true);
  renderQualityGateApp();

  // On new code
  await user.click(await screen.findByText('quality_gates.add_condition'));

  let dialog = within(screen.getByRole('dialog'));

  await user.click(dialog.getByRole('radio', { name: 'quality_gates.conditions.new_code' }));
  await user.click(dialog.getByRole('combobox'));
  await user.click(dialog.getByRole('option', { name: 'Issues' }));
  await user.click(dialog.getByRole('textbox', { name: 'quality_gates.conditions.value' }));
  await user.keyboard('12{Enter}');

  const newConditions = within(
    await screen.findByRole('table', { name: 'quality_gates.conditions.new_code.long' })
  );
  expect(await newConditions.findByRole('cell', { name: 'Issues' })).toBeInTheDocument();
  expect(await newConditions.findByRole('cell', { name: '12' })).toBeInTheDocument();

  // On overall code
  await user.click(await screen.findByText('quality_gates.add_condition'));

  dialog = within(screen.getByRole('dialog'));

  await user.click(dialog.getByLabelText('quality_gates.conditions.fails_when'));
  await user.click(dialog.getByRole('option', { name: 'Info Issues' }));
  await user.click(dialog.getByRole('radio', { name: 'quality_gates.conditions.overall_code' }));
  await user.click(dialog.getByLabelText('quality_gates.conditions.operator'));

  await user.click(dialog.getByText('quality_gates.operator.LT'));
  await user.click(dialog.getByRole('textbox', { name: 'quality_gates.conditions.value' }));
  await user.keyboard('42{Enter}');

  let overallConditions = within(
    await screen.findByRole('table', { name: 'quality_gates.conditions.overall_code.long' })
  );

  expect(await overallConditions.findByRole('cell', { name: 'Info Issues' })).toBeInTheDocument();
  expect(await overallConditions.findByRole('cell', { name: '42' })).toBeInTheDocument();

  // Select a rating
  await user.click(await screen.findByText('quality_gates.add_condition'));

  dialog = within(screen.getByRole('dialog'));
  await user.click(dialog.getByRole('radio', { name: 'quality_gates.conditions.overall_code' }));
  await user.click(dialog.getByLabelText('quality_gates.conditions.fails_when'));
  await user.click(dialog.getByRole('option', { name: 'Maintainability Rating' }));
  await user.click(dialog.getByLabelText('quality_gates.conditions.value'));
  await user.click(dialog.getByText('B'));
  await user.click(dialog.getByRole('button', { name: 'quality_gates.add_condition' }));

  overallConditions = within(
    await screen.findByRole('table', { name: 'quality_gates.conditions.overall_code.long' })
  );

  expect(
    await overallConditions.findByRole('cell', { name: 'Maintainability Rating' })
  ).toBeInTheDocument();
  expect(await overallConditions.findByRole('cell', { name: 'B' })).toBeInTheDocument();
});

it('should be able to edit a condition', async () => {
  const user = userEvent.setup();
  handler.setIsAdmin(true);
  renderQualityGateApp();

  const newConditions = within(
    await screen.findByRole('table', {
      name: 'quality_gates.conditions.new_code.long'
    })
  );

  await user.click(
    newConditions.getByLabelText('quality_gates.condition.edit.Coverage on New Code')
  );
  const dialog = within(screen.getByRole('dialog'));
  await user.click(dialog.getByRole('textbox', { name: 'quality_gates.conditions.value' }));
  await user.keyboard('{Backspace}{Backspace}23{Enter}');

  expect(await newConditions.findByText('Coverage')).toBeInTheDocument();
  expect(await newConditions.findByText('23.0%')).toBeInTheDocument();
});

it('should be able to handle duplicate or deprecated condition', async () => {
  const user = userEvent.setup();
  handler.setIsAdmin(true);
  renderQualityGateApp();
  await user.click(
    await screen.findByRole('menuitem', { name: handler.getCorruptedQualityGateName() })
  );

  expect(await screen.findByText('quality_gates.duplicated_conditions')).toBeInTheDocument();
  expect(
    await screen.findByRole('cell', { name: 'Complexity / Function deprecated' })
  ).toBeInTheDocument();
});

it('should be able to handle delete condition', async () => {
  const user = userEvent.setup();
  handler.setIsAdmin(true);
  renderQualityGateApp();

  const newConditions = within(
    await screen.findByRole('table', {
      name: 'quality_gates.conditions.new_code.long'
    })
  );

  await user.click(
    newConditions.getByLabelText('quality_gates.condition.delete.Coverage on New Code')
  );

  const dialog = within(screen.getByRole('dialog'));
  await user.click(dialog.getByRole('button', { name: 'delete' }));

  await waitFor(() => {
    expect(newConditions.queryByRole('cell', { name: 'Coverage' })).not.toBeInTheDocument();
  });
});

it('should explain condition on branch', async () => {
  renderQualityGateApp(mockAppState({ branchesEnabled: true }));

  expect(
    await screen.findByText('quality_gates.conditions.new_code.description')
  ).toBeInTheDocument();
  expect(
    await screen.findByText('quality_gates.conditions.overall_code.description')
  ).toBeInTheDocument();
});

function renderQualityGateApp(appState?: AppState) {
  renderApp('quality_gates', routes, { appState });
}
