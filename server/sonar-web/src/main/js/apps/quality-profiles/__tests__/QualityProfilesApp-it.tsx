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

  actionOnCQualityProfile: byRole('button', {
    name: 'quality_profiles.actions.c quality profile.C',
  }),
  extendButton: byRole('button', {
    name: 'extend',
  }),
  copyButton: byRole('button', {
    name: 'copy',
  }),
  createButton: byRole('button', { name: 'create' }),
  popup: byRole('dialog'),
  copyRadio: byRole('radio', {
    name: 'quality_profiles.creation_from_copy quality_profiles.creation_from_copy_description_1 quality_profiles.creation_from_copy_description_2',
  }),
  blankRadio: byRole('radio', {
    name: 'quality_profiles.creation_from_blank quality_profiles.creation_from_blank_description',
  }),
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

  await user.click(await ui.actionOnCQualityProfile.find());
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

  await user.click(await ui.actionOnCQualityProfile.find());
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

function renderQualityProfiles() {
  renderAppRoutes('profiles', routes, {
    languages: {
      js: { key: 'js', name: 'JavaScript' },
      java: { key: 'java', name: 'Java' },
      c: { key: 'c', name: 'C' },
    },
  });
}
