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
import { getByText } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import selectEvent from 'react-select-event';
import { byRole } from 'testing-library-selector';
import QualityProfilesServiceMock from '../../../api/mocks/QualityProfilesServiceMock';
import { renderAppRoutes } from '../../../helpers/testReactTestingUtils';
import routes from '../routes';

jest.mock('../../../api/quality-profiles');
jest.mock('../../../api/rules');

const serviceMock = new QualityProfilesServiceMock();

beforeEach(() => {
  serviceMock.reset();
});

const ui = {
  cQualityProfileName: 'c quality profile',
  newCQualityProfileName: 'New c quality profile',
  newCQualityProfileNameFromCreateButton: 'New c quality profile from create',
  profileActions: (name: string, language: string) =>
    byRole('button', {
      name: `quality_profiles.actions.${name}.${language}`,
    }),
  extendButton: byRole('button', {
    name: 'extend',
  }),
  copyButton: byRole('button', {
    name: 'copy',
  }),
  createButton: byRole('button', { name: 'create' }),
  compareButton: byRole('link', { name: 'compare' }),
  compareDropdown: byRole('textbox', { name: 'quality_profiles.compare_with' }),
  changelogLink: byRole('link', { name: 'changelog' }),
  popup: byRole('dialog'),
  copyRadio: byRole('radio', {
    name: 'quality_profiles.creation_from_copy quality_profiles.creation_from_copy_description_1 quality_profiles.creation_from_copy_description_2',
  }),
  blankRadio: byRole('radio', {
    name: 'quality_profiles.creation_from_blank quality_profiles.creation_from_blank_description',
  }),
  activeRuleButton: (profileName: string) =>
    byRole('button', {
      name: `quality_profiles.comparison.activate_rule.${profileName}`,
    }),
  activateConfirmButton: byRole('button', { name: 'coding_rules.activate' }),
  namePropupInput: byRole('textbox', { name: 'quality_profiles.new_name field_required' }),
  filterByLang: byRole('textbox', { name: 'quality_profiles.filter_by:' }),
  listLinkCQualityProfile: byRole('link', { name: 'c quality profile' }),
  listLinkNewCQualityProfile: byRole('link', { name: 'New c quality profile' }),
  listLinkNewCQualityProfileFromCreateButton: byRole('link', {
    name: 'New c quality profile from create',
  }),
  listLinkJavaQualityProfile: byRole('link', { name: 'java quality profile' }),
  returnToList: byRole('link', { name: 'quality_profiles.page' }),
  languageSelect: byRole('textbox', { name: 'language field_required' }),
  profileExtendSelect: byRole('textbox', {
    name: 'quality_profiles.creation.choose_parent_quality_profile field_required',
  }),
  profileCopySelect: byRole('textbox', {
    name: 'quality_profiles.creation.choose_copy_quality_profile field_required',
  }),
  nameCreatePopupInput: byRole('textbox', { name: 'name field_required' }),
  comparisonDiffTableHeading: (rulesQuantity: number, profileName: string) =>
    byRole('heading', { name: `quality_profiles.x_rules_only_in.${rulesQuantity} ${profileName}` }),
  comparisonModifiedTableHeading: (rulesQuantity: number) =>
    byRole('heading', {
      name: `quality_profiles.x_rules_have_different_configuration.${rulesQuantity}`,
    }),
};

it('should list Quality Profiles and filter by language', async () => {
  const user = userEvent.setup();
  serviceMock.setAdmin();
  renderQualityProfiles();
  expect(await ui.listLinkCQualityProfile.find()).toBeInTheDocument();
  expect(ui.listLinkJavaQualityProfile.get()).toBeInTheDocument();

  await selectEvent.select(ui.filterByLang.get(), 'C');

  expect(ui.listLinkJavaQualityProfile.query()).not.toBeInTheDocument();

  // Creation form should have language pre-selected
  await user.click(await ui.createButton.find());
  // eslint-disable-next-line testing-library/prefer-screen-queries
  expect(getByText(ui.popup.get(), 'C')).toBeInTheDocument();
  await selectEvent.select(ui.profileExtendSelect.get(), ui.cQualityProfileName);
  await user.type(ui.nameCreatePopupInput.get(), ui.newCQualityProfileName);

  expect(ui.createButton.get(ui.popup.get())).toBeEnabled();
});

it('should be able to extend Quality Profile', async () => {
  // From the list page
  const user = userEvent.setup();
  serviceMock.setAdmin();
  renderQualityProfiles();

  await user.click(await ui.profileActions('c quality profile', 'C').find());
  await user.click(ui.extendButton.get());

  await user.clear(ui.namePropupInput.get());
  await user.type(ui.namePropupInput.get(), ui.newCQualityProfileName);
  await user.click(ui.extendButton.get());
  expect(await ui.listLinkNewCQualityProfile.find()).toBeInTheDocument();

  // From the create form
  await user.click(ui.returnToList.get());
  await user.click(ui.createButton.get());

  await selectEvent.select(ui.languageSelect.get(), 'C');
  await selectEvent.select(ui.profileExtendSelect.get(), ui.newCQualityProfileName);
  await user.type(ui.nameCreatePopupInput.get(), ui.newCQualityProfileNameFromCreateButton);
  await user.click(ui.createButton.get(ui.popup.get()));

  expect(await ui.listLinkNewCQualityProfileFromCreateButton.find()).toBeInTheDocument();
});

it('should be able to copy Quality Profile', async () => {
  // From the list page
  const user = userEvent.setup();
  serviceMock.setAdmin();
  renderQualityProfiles();

  await user.click(await ui.profileActions('c quality profile', 'C').find());
  await user.click(ui.copyButton.get());

  await user.clear(ui.namePropupInput.get());
  await user.type(ui.namePropupInput.get(), ui.newCQualityProfileName);
  await user.click(ui.copyButton.get(ui.popup.get()));
  expect(await ui.listLinkNewCQualityProfile.find()).toBeInTheDocument();

  // From the create form
  await user.click(ui.returnToList.get());
  await user.click(ui.createButton.get());

  await user.click(ui.copyRadio.get());
  await selectEvent.select(ui.languageSelect.get(), 'C');
  await selectEvent.select(ui.profileCopySelect.get(), ui.newCQualityProfileName);
  await user.type(ui.nameCreatePopupInput.get(), ui.newCQualityProfileNameFromCreateButton);
  await user.click(ui.createButton.get(ui.popup.get()));

  expect(await ui.listLinkNewCQualityProfileFromCreateButton.find()).toBeInTheDocument();
});

it('should be able to create blank Quality Profile', async () => {
  // From the list page
  const user = userEvent.setup();
  serviceMock.setAdmin();
  renderQualityProfiles();

  await user.click(await ui.createButton.find());

  await user.click(ui.blankRadio.get());
  await selectEvent.select(ui.languageSelect.get(), 'C');
  await user.type(ui.nameCreatePopupInput.get(), ui.newCQualityProfileName);
  await user.click(ui.createButton.get(ui.popup.get()));

  expect(await ui.listLinkNewCQualityProfile.find()).toBeInTheDocument();
});

it('should be able to compare profiles', async () => {
  // From the list page
  const user = userEvent.setup();
  serviceMock.setAdmin();
  renderQualityProfiles();

  // For language with 1 profle we should not see compare action
  await user.click(await ui.profileActions('c quality profile', 'C').find());
  expect(ui.compareButton.query()).not.toBeInTheDocument();

  await user.click(ui.profileActions('java quality profile', 'Java').get());
  expect(ui.compareButton.get()).toBeInTheDocument();
  await user.click(ui.compareButton.get());
  expect(ui.compareDropdown.get()).toBeInTheDocument();
  expect(ui.profileActions('java quality profile', 'Java').query()).not.toBeInTheDocument();
  expect(ui.changelogLink.query()).not.toBeInTheDocument();

  await selectEvent.select(ui.compareDropdown.get(), 'java quality profile #2');
  expect(ui.comparisonDiffTableHeading(1, 'java quality profile').get()).toBeInTheDocument();
  expect(ui.comparisonDiffTableHeading(1, 'java quality profile #2').get()).toBeInTheDocument();
  expect(ui.comparisonModifiedTableHeading(1).query()).toBeInTheDocument();

  // java quality profile is not editable
  expect(ui.activeRuleButton('java quality profile').query()).not.toBeInTheDocument();

  await user.click(ui.activeRuleButton('java quality profile #2').get());
  expect(ui.popup.get()).toBeInTheDocument();

  await user.click(ui.activateConfirmButton.get());
  expect(ui.comparisonDiffTableHeading(1, 'java quality profile').query()).not.toBeInTheDocument();
});

function renderQualityProfiles() {
  renderAppRoutes('profiles', routes, {
    languages: {
      js: { key: 'js', name: 'JavaScript' },
      java: { key: 'java', name: 'Java' },
      c: { key: 'c', name: 'C' },
    },
  });
}
