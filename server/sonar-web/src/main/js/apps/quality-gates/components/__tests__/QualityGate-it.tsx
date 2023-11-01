/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import selectEvent from 'react-select-event';
import { QualityGatesServiceMock } from '../../../../api/mocks/QualityGatesServiceMock';
import UsersServiceMock from '../../../../api/mocks/UsersServiceMock';
import { searchProjects, searchUsers } from '../../../../api/quality-gates';
import { dismissNotice } from '../../../../api/users';
import { mockLoggedInUser } from '../../../../helpers/testMocks';
import { RenderContext, renderAppRoutes } from '../../../../helpers/testReactTestingUtils';
import { byRole } from '../../../../helpers/testSelector';
import { Feature } from '../../../../types/features';
import { NoticeType } from '../../../../types/users';
import routes from '../../routes';

let qualityGateHandler: QualityGatesServiceMock;
let usersHandler: UsersServiceMock;

beforeAll(() => {
  qualityGateHandler = new QualityGatesServiceMock();
  usersHandler = new UsersServiceMock();
});

afterEach(() => {
  qualityGateHandler.reset();
  usersHandler.reset();
});

it('should open the default quality gates', async () => {
  renderQualityGateApp();

  const defaultQualityGate = qualityGateHandler.getDefaultQualityGate();
  expect(
    await screen.findByRole('button', {
      current: 'page',
      name: `${defaultQualityGate.name} default`,
    }),
  ).toBeInTheDocument();
});

it('should list all quality gates', async () => {
  renderQualityGateApp();

  expect(
    await screen.findByRole('button', {
      name: `${qualityGateHandler.getDefaultQualityGate().name} default`,
    }),
  ).toBeInTheDocument();

  expect(
    screen.getByRole('button', {
      name: `${qualityGateHandler.getBuiltInQualityGate().name} quality_gates.built_in`,
    }),
  ).toBeInTheDocument();
});

it('should render the built-in quality gate properly', async () => {
  const user = userEvent.setup();
  renderQualityGateApp();

  const builtInQualityGate = await screen.findByText('Sonar way');

  await act(async () => {
    await user.click(builtInQualityGate);
  });

  expect(await screen.findByText(/quality_gates.is_built_in.description/)).toBeInTheDocument();
});

it('should be able to create a quality gate then delete it', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();
  let createButton = await screen.findByRole('button', { name: 'create' });

  // Using keyboard
  await user.click(createButton);
  await act(async () => {
    await user.click(screen.getByRole('textbox', { name: /name.*/ }));
    await user.keyboard('testone');
    await user.click(screen.getByRole('button', { name: 'quality_gate.create' }));
  });
  expect(await screen.findByRole('button', { name: 'testone' })).toBeInTheDocument();

  // Using modal button
  createButton = await screen.findByRole('button', { name: 'create' });
  await user.click(createButton);
  const saveButton = screen.getByRole('button', { name: 'quality_gate.create' });

  expect(saveButton).toBeDisabled();
  const nameInput = screen.getByRole('textbox', { name: /name.*/ });
  await act(async () => {
    await user.click(nameInput);
    await user.keyboard('testtwo');
    await user.click(saveButton);
  });

  const newQG = await screen.findByRole('button', { name: 'testtwo' });

  expect(newQG).toBeInTheDocument();

  // Delete the quality gate
  await user.click(newQG);

  await user.click(screen.getByLabelText('menu'));
  const deleteButton = screen.getByRole('menuitem', { name: 'delete' });
  await user.click(deleteButton);
  const popup = screen.getByRole('dialog');
  const dialogDeleteButton = within(popup).getByRole('button', { name: 'delete' });
  await user.click(dialogDeleteButton);

  await waitFor(() => {
    expect(screen.queryByRole('button', { name: 'testtwo' })).not.toBeInTheDocument();
  });
});

it('should be able to copy a quality gate which is CAYC compliant', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  const notDefaultQualityGate = await screen.findByText('Sonar way');
  await user.click(notDefaultQualityGate);
  await user.click(screen.getByLabelText('menu'));
  const copyButton = screen.getByRole('menuitem', { name: 'copy' });

  await user.click(copyButton);
  const nameInput = screen.getByRole('textbox', { name: /name.*/ });
  expect(nameInput).toBeInTheDocument();
  await act(async () => {
    await user.click(nameInput);
    await user.keyboard(' bis{Enter}');
  });
  expect(await screen.findByRole('button', { name: /.* bis/ })).toBeInTheDocument();
});

it('should not be able to copy a quality gate which is not CAYC compliant', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  const notDefaultQualityGate = await screen.findByText('SonarSource way - CFamily');
  await user.click(notDefaultQualityGate);
  await user.click(screen.getByLabelText('menu'));
  const copyButton = screen.getByRole('menuitem', { name: 'copy' });

  expect(copyButton).toBeDisabled();
});

it('should be able to rename a quality gate', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();
  await user.click(await screen.findByLabelText('menu'));
  const renameButton = screen.getByRole('menuitem', { name: 'rename' });

  await user.click(renameButton);
  const nameInput = screen.getByRole('textbox', { name: /name.*/ });
  expect(nameInput).toBeInTheDocument();
  await user.click(nameInput);
  await user.keyboard('{Control>}a{/Control}New Name{Enter}');

  expect(await screen.findByRole('button', { name: /New Name.*/ })).toBeInTheDocument();
});

it('should not be able to set as default a quality gate which is not CAYC compliant', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  const notDefaultQualityGate = await screen.findByText('SonarSource way - CFamily');
  await user.click(notDefaultQualityGate);
  await user.click(screen.getByLabelText('menu'));
  const setAsDefaultButton = screen.getByRole('menuitem', { name: 'set_as_default' });
  expect(setAsDefaultButton).toBeDisabled();
});

it('should be able to set as default a quality gate which is CAYC compliant', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  const notDefaultQualityGate = await screen.findByRole('button', { name: /Sonar way/ });
  await user.click(notDefaultQualityGate);
  await user.click(screen.getByLabelText('menu'));
  const setAsDefaultButton = screen.getByRole('menuitem', { name: 'set_as_default' });
  await user.click(setAsDefaultButton);
  expect(screen.getByRole('button', { name: /Sonar way default/ })).toBeInTheDocument();
});

it('should be able to add a condition', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  await user.click(await screen.findByText('SonarSource way - CFamily'));

  // On new code
  await user.click(await screen.findByText('quality_gates.add_condition'));

  let dialog = within(screen.getByRole('dialog'));

  await user.click(dialog.getByRole('radio', { name: 'quality_gates.conditions.new_code' }));
  await selectEvent.select(dialog.getByRole('combobox'), ['Issues']);
  await user.click(dialog.getByRole('textbox', { name: 'quality_gates.conditions.value' }));
  await user.keyboard('12');
  await user.click(dialog.getByRole('button', { name: 'quality_gates.add_condition' }));
  const newConditions = within(await screen.findByTestId('quality-gates__conditions-new'));
  expect(await newConditions.findByRole('cell', { name: 'Issues' })).toBeInTheDocument();
  expect(await newConditions.findByRole('cell', { name: '12' })).toBeInTheDocument();

  // On overall code
  await user.click(await screen.findByText('quality_gates.add_condition'));

  dialog = within(screen.getByRole('dialog'));
  await selectEvent.select(dialog.getByRole('combobox'), ['Info Issues']);
  await user.click(dialog.getByRole('radio', { name: 'quality_gates.conditions.overall_code' }));
  await user.click(dialog.getByLabelText('quality_gates.conditions.operator'));

  await user.click(dialog.getByText('quality_gates.operator.LT'));
  await user.click(dialog.getByRole('textbox', { name: 'quality_gates.conditions.value' }));
  await user.keyboard('42');
  await user.click(dialog.getByRole('button', { name: 'quality_gates.add_condition' }));

  const overallConditions = within(await screen.findByTestId('quality-gates__conditions-overall'));

  expect(await overallConditions.findByRole('cell', { name: 'Info Issues' })).toBeInTheDocument();
  expect(await overallConditions.findByRole('cell', { name: '42' })).toBeInTheDocument();

  // Select a rating
  await user.click(await screen.findByText('quality_gates.add_condition'));

  dialog = within(screen.getByRole('dialog'));
  await user.click(dialog.getByRole('radio', { name: 'quality_gates.conditions.overall_code' }));
  await selectEvent.select(dialog.getByRole('combobox'), ['Maintainability Rating']);
  await user.click(dialog.getByLabelText('quality_gates.conditions.value'));
  await user.click(dialog.getByText('B'));
  await user.click(dialog.getByRole('button', { name: 'quality_gates.add_condition' }));

  expect(
    await overallConditions.findByRole('cell', { name: 'Maintainability Rating' }),
  ).toBeInTheDocument();
  expect(await overallConditions.findByRole('cell', { name: 'B' })).toBeInTheDocument();
});

it('should be able to edit a condition', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  const newConditions = within(await screen.findByTestId('quality-gates__conditions-new'));

  await user.click(
    newConditions.getByLabelText('quality_gates.condition.edit.Line Coverage on New Code'),
  );
  const dialog = within(screen.getByRole('dialog'));
  const textBox = dialog.getByRole('textbox', { name: 'quality_gates.conditions.value' });
  await user.clear(textBox);
  await user.type(textBox, '23');
  await user.click(dialog.getByRole('button', { name: 'quality_gates.update_condition' }));
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

  expect(await newConditions.findByText('Line Coverage')).toBeInTheDocument();
  expect(
    await within(await screen.findByTestId('quality-gates__conditions-new')).findByText('23.0%'),
  ).toBeInTheDocument();
});

it('should be able to handle duplicate or deprecated condition', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  await user.click(
    // make it a regexp to ignore badges:
    await screen.findByRole('button', {
      name: new RegExp(qualityGateHandler.getCorruptedQualityGateName()),
    }),
  );

  expect(await screen.findByText('quality_gates.duplicated_conditions')).toBeInTheDocument();
  expect(
    await screen.findByRole('cell', { name: 'Complexity / Function deprecated' }),
  ).toBeInTheDocument();
});

it('should be able to handle delete condition', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  await user.click(await screen.findByText('Non Cayc QG'));
  const newConditions = within(await screen.findByTestId('quality-gates__conditions-new'));

  await user.click(
    newConditions.getByLabelText('quality_gates.condition.delete.Coverage on New Code'),
  );

  const dialog = within(screen.getByRole('dialog'));
  await user.click(dialog.getByRole('button', { name: 'delete' }));

  await waitFor(() => {
    expect(newConditions.queryByRole('cell', { name: 'Coverage' })).not.toBeInTheDocument();
  });
});

it('should explain condition on branch', async () => {
  renderQualityGateApp({ featureList: [Feature.BranchSupport] });

  expect(
    await screen.findByText('quality_gates.conditions.new_code.description'),
  ).toBeInTheDocument();
  expect(
    await screen.findByText('quality_gates.conditions.overall_code.description'),
  ).toBeInTheDocument();
});

it('should show warning banner when CAYC condition is not properly set and should be able to update them', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  const qualityGate = await screen.findByText('SonarSource way - CFamily');

  await user.click(qualityGate);

  expect(screen.getByText('quality_gates.cayc_missing.banner.title')).toBeInTheDocument();
  expect(screen.getByText('quality_gates.cayc_missing.banner.description')).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'quality_gates.cayc_condition.review_update' }),
  ).toBeInTheDocument();

  await user.click(
    screen.getByRole('button', { name: 'quality_gates.cayc_condition.review_update' }),
  );
  expect(
    screen.getByRole('dialog', {
      name: 'quality_gates.cayc.review_update_modal.header.SonarSource way - CFamily',
    }),
  ).toBeInTheDocument();
  expect(
    screen.getByText('quality_gates.cayc.review_update_modal.description1'),
  ).toBeInTheDocument();
  expect(
    screen.getByText('quality_gates.cayc.review_update_modal.description2'),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'quality_gates.cayc.review_update_modal.confirm_text' }),
  ).toBeInTheDocument();

  await user.click(
    screen.getByRole('button', { name: 'quality_gates.cayc.review_update_modal.confirm_text' }),
  );

  expect(await screen.findByText('quality_gates.cayc.banner.title')).toBeInTheDocument();

  const overallConditionsWrapper = within(
    await screen.findByTestId('quality-gates__conditions-overall'),
  );
  expect(overallConditionsWrapper.getByText('Complexity / Function')).toBeInTheDocument();
});

it('should not warn user when quality gate is not CAYC compliant and user has no permission to edit it', async () => {
  const user = userEvent.setup();
  renderQualityGateApp();

  const nonCompliantQualityGate = await screen.findByRole('button', { name: 'Non Cayc QG' });

  await user.click(nonCompliantQualityGate);

  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  expect(screen.queryByText('quality_gates.cayc.tooltip.message')).not.toBeInTheDocument();
});

it('should warn user when quality gate is not CAYC compliant and user has permission to edit it', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  const nonCompliantQualityGate = await screen.findByRole('button', { name: /Non Cayc QG/ });

  await user.click(nonCompliantQualityGate);

  expect(await screen.findByText(/quality_gates.cayc_missing.banner.title/)).toBeInTheDocument();
  expect(screen.getAllByText('quality_gates.cayc.tooltip.message').length).toBeGreaterThan(0);
});

it('should render CaYC conditions on a separate table if Sonar way', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();
  await user.click(await screen.findByText('Sonar way'));

  expect(screen.queryByText('quality_gates.cayc.banner.title')).not.toBeInTheDocument();
  expect(await screen.findByTestId('quality-gates__conditions-cayc')).toBeInTheDocument();
});

it('should display CaYC condition simplification tour for users who didnt dismissed it', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp({ currentUser: mockLoggedInUser() });

  const qualityGate = await screen.findByText('Sonar way');

  await act(async () => {
    await user.click(qualityGate);
  });

  expect(await byRole('alertdialog').find()).toBeInTheDocument();

  expect(
    byRole('alertdialog')
      .byText('quality_gates.cayc.condition_simplification_tour.page_1.title')
      .get(),
  ).toBeInTheDocument();

  await act(async () => {
    await user.click(byRole('alertdialog').byRole('button', { name: 'next' }).get());
  });

  expect(
    byRole('alertdialog')
      .byText('quality_gates.cayc.condition_simplification_tour.page_2.title')
      .get(),
  ).toBeInTheDocument();

  await act(async () => {
    await user.click(byRole('alertdialog').byRole('button', { name: 'next' }).get());
  });

  expect(
    byRole('alertdialog')
      .byText('quality_gates.cayc.condition_simplification_tour.page_3.title')
      .get(),
  ).toBeInTheDocument();

  await act(async () => {
    await user.click(byRole('alertdialog').byRole('button', { name: 'dismiss' }).get());
  });

  expect(byRole('alertdialog').query()).not.toBeInTheDocument();
  expect(dismissNotice).toHaveBeenLastCalledWith(NoticeType.QG_CAYC_CONDITIONS_SIMPLIFICATION);
});

it('should not display CaYC condition simplification tour for users who dismissed it', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp({
    currentUser: mockLoggedInUser({
      dismissedNotices: { [NoticeType.QG_CAYC_CONDITIONS_SIMPLIFICATION]: true },
    }),
  });

  const qualityGate = await screen.findByText('Sonar way');

  await act(async () => {
    await user.click(qualityGate);
  });

  expect(byRole('alertdialog').query()).not.toBeInTheDocument();
});

describe('The Project section', () => {
  it('should render list of projects correctly in different tabs', async () => {
    const user = userEvent.setup();
    qualityGateHandler.setIsAdmin(true);
    renderQualityGateApp();

    const notDefaultQualityGate = await screen.findByText('SonarSource way - CFamily');

    await user.click(notDefaultQualityGate);

    // by default it shows "selected" values
    expect(screen.getAllByRole('checkbox')).toHaveLength(2);

    // change tabs to show deselected projects
    await user.click(screen.getByRole('radio', { name: 'quality_gates.projects.without' }));
    expect(screen.getAllByRole('checkbox')).toHaveLength(2);

    // change tabs to show all projects
    await user.click(screen.getByRole('radio', { name: 'quality_gates.projects.all' }));
    expect(screen.getAllByRole('checkbox')).toHaveLength(4);
  });

  it('should handle select and deselect correctly', async () => {
    const user = userEvent.setup();
    qualityGateHandler.setIsAdmin(true);
    renderQualityGateApp();

    const notDefaultQualityGate = await screen.findByText('SonarSource way - CFamily');

    await user.click(notDefaultQualityGate);

    const checkedProjects = screen.getAllByRole('checkbox')[0];
    expect(screen.getAllByRole('checkbox')).toHaveLength(2);
    await user.click(checkedProjects);
    const reloadButton = screen.getByRole('button', { name: 'reload' });
    expect(reloadButton).toBeInTheDocument();
    await user.click(reloadButton);

    // FP
    // eslint-disable-next-line jest-dom/prefer-in-document
    expect(screen.getAllByRole('checkbox')).toHaveLength(1);

    // change tabs to show deselected projects
    await user.click(screen.getByRole('radio', { name: 'quality_gates.projects.without' }));

    const uncheckedProjects = screen.getAllByRole('checkbox')[0];
    expect(screen.getAllByRole('checkbox')).toHaveLength(3);
    await user.click(uncheckedProjects);
    expect(reloadButton).toBeInTheDocument();
    await user.click(reloadButton);
    expect(screen.getAllByRole('checkbox')).toHaveLength(2);
  });

  it('should handle the search of projects', async () => {
    const user = userEvent.setup();
    qualityGateHandler.setIsAdmin(true);
    renderQualityGateApp();

    const notDefaultQualityGate = await screen.findByText('SonarSource way - CFamily');

    await user.click(notDefaultQualityGate);

    const searchInput = screen.getByRole('searchbox', { name: 'search_verb' });
    expect(searchInput).toBeInTheDocument();
    await user.click(searchInput);
    await user.keyboard('test2{Enter}');

    // FP
    // eslint-disable-next-line jest-dom/prefer-in-document
    expect(screen.getAllByRole('checkbox')).toHaveLength(1);
  });

  it('should display show more button if there are multiple pages of data', async () => {
    (searchProjects as jest.Mock).mockResolvedValueOnce({
      paging: { pageIndex: 2, pageSize: 3, total: 55 },
      results: [],
    });

    const user = userEvent.setup();
    qualityGateHandler.setIsAdmin(true);
    renderQualityGateApp();

    const notDefaultQualityGate = await screen.findByText('SonarSource way - CFamily');
    await user.click(notDefaultQualityGate);

    expect(screen.getByRole('button', { name: 'show_more' })).toBeInTheDocument();
  });
});

describe('The Permissions section', () => {
  it('should not show button to grant permission when user is not admin', async () => {
    renderQualityGateApp();

    // await just to make sure we've loaded the page
    expect(
      await screen.findByRole('button', {
        name: `${qualityGateHandler.getDefaultQualityGate().name} default`,
      }),
    ).toBeInTheDocument();

    expect(screen.queryByText('quality_gates.permissions')).not.toBeInTheDocument();
  });
  it('should show button to grant permission when user is admin', async () => {
    qualityGateHandler.setIsAdmin(true);
    renderQualityGateApp();

    const grantPermissionButton = await screen.findByRole('button', {
      name: 'quality_gates.permissions.grant',
    });
    expect(screen.getByText('quality_gates.permissions')).toBeInTheDocument();
    expect(grantPermissionButton).toBeInTheDocument();
  });

  it('should assign permission to a user and delete it later', async () => {
    const user = userEvent.setup();
    qualityGateHandler.setIsAdmin(true);
    renderQualityGateApp();

    expect(screen.queryByText('userlogin')).not.toBeInTheDocument();

    // Granting permission to a user
    const grantPermissionButton = await screen.findByRole('button', {
      name: 'quality_gates.permissions.grant',
    });
    await user.click(grantPermissionButton);
    const popup = screen.getByRole('dialog');
    const searchUserInput = within(popup).getByRole('combobox', {
      name: 'quality_gates.permissions.search',
    });
    expect(searchUserInput).toBeInTheDocument();
    const addUserButton = screen.getByRole('button', {
      name: 'add_verb',
    });
    expect(addUserButton).toBeDisabled();
    await user.click(searchUserInput);
    await user.click(screen.getByText('userlogin'));
    expect(addUserButton).toBeEnabled();
    await user.click(addUserButton);
    expect(screen.getByText('userlogin')).toBeInTheDocument();

    // Cancel granting permission
    await user.click(grantPermissionButton);
    await user.click(searchUserInput);
    await user.keyboard('test{Enter}');

    const cancelButton = screen.getByRole('button', {
      name: 'cancel',
    });
    await user.click(cancelButton);

    const permissionList = within(await screen.findByTestId('quality-gate-permissions'));
    expect(permissionList.getByRole('row')).toBeInTheDocument();

    // Delete the user permission
    const deleteButton = screen.getByTestId('permission-delete-button');
    await user.click(deleteButton);
    const deletePopup = screen.getByRole('dialog');
    const dialogDeleteButton = within(deletePopup).getByRole('button', { name: 'remove' });
    await user.click(dialogDeleteButton);
    expect(permissionList.queryByRole('row')).not.toBeInTheDocument();
  });

  it('should assign permission to a group and delete it later', async () => {
    const user = userEvent.setup();
    qualityGateHandler.setIsAdmin(true);
    renderQualityGateApp();

    expect(screen.queryByText('userlogin')).not.toBeInTheDocument();

    // Granting permission to a group
    const grantPermissionButton = await screen.findByRole('button', {
      name: 'quality_gates.permissions.grant',
    });
    await user.click(grantPermissionButton);
    const popup = screen.getByRole('dialog');
    const searchUserInput = within(popup).getByRole('combobox', {
      name: 'quality_gates.permissions.search',
    });
    const addUserButton = screen.getByRole('button', {
      name: 'add_verb',
    });
    await user.click(searchUserInput);
    await user.click(within(popup).getByLabelText('Foo'));
    await user.click(addUserButton);
    expect(screen.getByText('Foo')).toBeInTheDocument();

    // Delete the group permission
    const deleteButton = screen.getByTestId('permission-delete-button');
    await user.click(deleteButton);
    const deletePopup = screen.getByRole('dialog');
    const dialogDeleteButton = within(deletePopup).getByRole('button', { name: 'remove' });
    await user.click(dialogDeleteButton);
    const permissionList = within(await screen.findByTestId('quality-gate-permissions'));
    expect(permissionList.queryByRole('listitem')).not.toBeInTheDocument();
  });

  it('should handle searchUser service failure', async () => {
    (searchUsers as jest.Mock).mockRejectedValue('error');

    const user = userEvent.setup();
    qualityGateHandler.setIsAdmin(true);
    renderQualityGateApp();

    const grantPermissionButton = await screen.findByRole('button', {
      name: 'quality_gates.permissions.grant',
    });
    await user.click(grantPermissionButton);
    const popup = screen.getByRole('dialog');
    const searchUserInput = within(popup).getByRole('combobox', {
      name: 'quality_gates.permissions.search',
    });
    await user.click(searchUserInput);

    expect(screen.getByText('no_results')).toBeInTheDocument();
  });
});

function renderQualityGateApp(context?: RenderContext) {
  return renderAppRoutes('quality_gates', routes, context);
}
