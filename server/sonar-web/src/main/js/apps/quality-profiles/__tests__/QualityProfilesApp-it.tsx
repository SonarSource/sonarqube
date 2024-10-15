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
import { byLabelText, byRole, byText } from '~sonar-aligned/helpers/testSelector';
import QualityProfilesServiceMock from '../../../api/mocks/QualityProfilesServiceMock';
import SettingsServiceMock from '../../../api/mocks/SettingsServiceMock';
import { mockCompareResult, mockPaging, mockRule } from '../../../helpers/testMocks';
import { renderAppRoutes } from '../../../helpers/testReactTestingUtils';
import routes from '../routes';

jest.mock('../../../api/quality-profiles');
jest.mock('../../../api/rules');

const serviceMock = new QualityProfilesServiceMock();
const settingsMock = new SettingsServiceMock();

beforeEach(() => {
  serviceMock.reset();
  settingsMock.reset();
});

const ui = {
  cQualityProfileName: 'c quality profile',
  newCQualityProfileName: 'New c quality profile',
  newCQualityProfileNameFromCreateButton: 'New c quality profile from create',
  listProfileActions: (name: string, language: string) =>
    byRole('button', {
      name: `quality_profiles.actions.${name}.${language}`,
    }),
  profileActions: (name: string, language: string) =>
    byRole('menuitem', {
      name: `quality_profiles.actions.${name}.${language}`,
    }),
  modalExtendButton: byRole('button', {
    name: 'extend',
  }),
  qualityProfileActions: byRole('button', {
    name: /quality_profiles.actions/,
  }),
  extendButton: byRole('menuitem', {
    name: 'extend',
  }),
  modalCopyButton: byRole('button', {
    name: 'copy',
  }),
  copyButton: byRole('menuitem', {
    name: 'copy',
  }),
  createButton: byRole('button', { name: 'create' }),
  restoreButton: byRole('button', { name: 'restore' }),
  compareButton: byRole('menuitem', { name: 'compare' }),
  cancelButton: byRole('button', { name: 'cancel' }),
  compareDropdown: byRole('combobox', { name: 'quality_profiles.compare_with' }),
  changelogLink: byRole('link', { name: 'changelog' }),
  popup: byRole('dialog'),
  confirmationModal: byRole('alertdialog'),
  restoreProfileDialog: byRole('dialog', { name: 'quality_profiles.restore_profile' }),
  copyRadio: byRole('radio', {
    name: 'quality_profiles.creation_from_copy quality_profiles.creation_from_copy_description_1 quality_profiles.creation_from_copy_description_2',
  }),
  extendRadio: byRole('radio', {
    name: 'quality_profiles.creation_from_extend quality_profiles.creation_from_extend_description_1 quality_profiles.creation_from_extend_description_2',
  }),
  blankRadio: byRole('radio', {
    name: 'quality_profiles.creation_from_blank quality_profiles.creation_from_blank_description',
  }),
  activeRuleButton: (profileName: string) =>
    byRole('button', {
      name: `quality_profiles.comparison.activate_rule.${profileName}`,
    }),
  deactivateRuleButton: (profileName: string) =>
    byRole('button', {
      name: `quality_profiles.comparison.deactivate_rule.${profileName}`,
    }),
  deactivateConfirmButton: byRole('button', { name: 'yes' }),
  activateConfirmButton: byRole('button', { name: 'coding_rules.activate' }),
  namePropupInput: byRole('textbox', { name: 'quality_profiles.new_name required' }),
  filterByLang: byRole('combobox', { name: 'quality_profiles.select_lang' }),
  listLinkCQualityProfile: byRole('link', { name: 'c quality profile' }),
  headingNewCQualityProfile: byRole('heading', { name: 'New c quality profile' }),
  headingNewCQualityProfileFromCreateButton: byRole('heading', {
    name: 'New c quality profile from create',
  }),
  listLinkJavaQualityProfile: byRole('link', { name: 'java quality profile' }),
  returnToList: byRole('link', { name: 'quality_profiles.page' }),
  languageSelect: byRole('combobox', { name: 'language' }),
  profileExtendSelect: byLabelText('quality_profiles.creation.choose_parent_quality_profile'),
  profileCopySelect: byLabelText('quality_profiles.creation.choose_copy_quality_profile'),
  nameCreatePopupInput: byRole('textbox', { name: 'name required' }),
  importerA: byText('Importer A'),
  importerB: byText('Importer B'),
  summaryAdditionalRules: (count: number) => byText(`quality_profile.summary_additional.${count}`),
  summaryFewerRules: (count: number) => byText(`quality_profile.summary_fewer.${count}`),
  comparisonDiffTableHeading: (rulesQuantity: number, profileName: string) =>
    byRole('columnheader', {
      name: `quality_profiles.x_rules_only_in.${rulesQuantity}.${profileName}`,
    }),
  comparisonModifiedTableHeading: (rulesQuantity: number) =>
    byRole('table', {
      name: `quality_profiles.x_rules_have_different_configuration.${rulesQuantity}`,
    }),
  deprecatedRulesRegion: byRole('region', { name: 'quality_profiles.deprecated_rules' }),
  stagnantProfilesRegion: byRole('region', { name: 'quality_profiles.stagnant_profiles' }),
  recentlyAddedRulesRegion: byRole('region', { name: 'quality_profiles.latest_new_rules' }),
  newRuleLink: byRole('link', { name: 'Recently Added Rule' }),
  seeAllNewRulesLink: byRole('link', { name: 'quality_profiles.latest_new_rules.see_all_x.20' }),
};

it('should list Quality Profiles and filter by language', async () => {
  const user = userEvent.setup();
  serviceMock.setAdmin();
  renderQualityProfiles();
  expect(await ui.listLinkCQualityProfile.find()).toBeInTheDocument();
  expect(ui.listLinkJavaQualityProfile.get()).toBeInTheDocument();

  await user.click(ui.filterByLang.get());
  await user.click(byRole('option', { name: 'C' }).get());

  expect(ui.listLinkJavaQualityProfile.query()).not.toBeInTheDocument();

  // Creation form should have language pre-selected
  await user.click(await ui.createButton.find());

  expect(ui.languageSelect.get()).toHaveValue('C');
});

describe('Evolution', () => {
  it('should list deprecated rules', async () => {
    serviceMock.setAdmin();
    renderQualityProfiles();

    expect(await ui.deprecatedRulesRegion.find()).toBeInTheDocument();
    expect(
      ui.deprecatedRulesRegion.byRole('link', { name: 'Good old PHP quality profile' }).get(),
    ).toBeInTheDocument();
    expect(
      ui.deprecatedRulesRegion.byRole('link', { name: 'java quality profile #2' }).get(),
    ).toBeInTheDocument();
  });

  it('should list stagnant profiles', async () => {
    serviceMock.setAdmin();
    renderQualityProfiles();

    expect(await ui.stagnantProfilesRegion.find()).toBeInTheDocument();
    expect(
      ui.stagnantProfilesRegion.byRole('link', { name: 'Good old PHP quality profile' }).get(),
    ).toBeInTheDocument();
  });

  it('should list recently added rules', async () => {
    serviceMock.setAdmin();
    serviceMock.setRulesSearchResponse({
      rules: [mockRule({ name: 'Recently Added Rule' })],
      paging: mockPaging({
        total: 20,
      }),
    });
    renderQualityProfiles();

    expect(await ui.recentlyAddedRulesRegion.find()).toBeInTheDocument();
    expect(ui.newRuleLink.get()).toBeInTheDocument();
    expect(ui.seeAllNewRulesLink.get()).toBeInTheDocument();
  });
});

describe('Create', () => {
  it('should be able to extend an existing Quality Profile', async () => {
    const user = userEvent.setup();
    serviceMock.setAdmin();
    renderQualityProfiles();

    await user.click(await ui.listProfileActions('c quality profile', 'C').find());
    await user.click(ui.extendButton.get());
    await user.clear(ui.namePropupInput.get());
    await user.type(ui.namePropupInput.get(), ui.newCQualityProfileName);
    await user.keyboard('{Enter}');

    expect(await ui.headingNewCQualityProfile.find()).toBeInTheDocument();

    await user.click(ui.returnToList.get());
    await user.click(ui.createButton.get());
    await user.click(ui.extendRadio.get());

    await user.click(ui.languageSelect.get());
    await user.click(byRole('option', { name: 'C' }).get());

    await user.click(ui.profileExtendSelect.get());
    await user.click(byRole('option', { name: ui.newCQualityProfileName }).get());

    await user.type(ui.nameCreatePopupInput.get(), ui.newCQualityProfileNameFromCreateButton);
    await user.click(ui.createButton.get(ui.popup.get()));

    expect(await ui.headingNewCQualityProfileFromCreateButton.find()).toBeInTheDocument();
  });

  it('should be able to copy an existing Quality Profile', async () => {
    const user = userEvent.setup();
    serviceMock.setAdmin();
    renderQualityProfiles();

    await user.click(await ui.listProfileActions('c quality profile', 'C').find());
    await user.click(ui.copyButton.get());
    await user.clear(ui.namePropupInput.get());
    await user.type(ui.namePropupInput.get(), ui.newCQualityProfileName);
    await user.click(ui.modalCopyButton.get(ui.popup.get()));

    expect(await ui.headingNewCQualityProfile.find()).toBeInTheDocument();

    await user.click(ui.returnToList.get());
    await user.click(ui.createButton.get());
    await user.click(ui.copyRadio.get());

    await user.click(ui.languageSelect.get());
    await user.click(byRole('option', { name: 'C' }).get());

    await user.click(ui.profileCopySelect.get());
    await user.click(byRole('option', { name: ui.newCQualityProfileName }).get());

    await user.type(ui.nameCreatePopupInput.get(), ui.newCQualityProfileNameFromCreateButton);
    await user.click(ui.createButton.get(ui.popup.get()));

    expect(await ui.headingNewCQualityProfileFromCreateButton.find()).toBeInTheDocument();
  });

  it('should be able to create blank Quality Profile', async () => {
    const user = userEvent.setup();
    serviceMock.setAdmin();
    renderQualityProfiles();

    await user.click(await ui.createButton.find());
    await user.click(ui.blankRadio.get());

    await user.click(ui.languageSelect.get());
    await user.click(byRole('option', { name: 'C' }).get());

    await user.type(ui.nameCreatePopupInput.get(), ui.newCQualityProfileName);
    await user.click(ui.createButton.get(ui.popup.get()));

    expect(await ui.headingNewCQualityProfile.find()).toBeInTheDocument();
  });

  it('should render importers', async () => {
    const user = userEvent.setup();
    serviceMock.setAdmin();
    renderQualityProfiles();

    await user.click(await ui.createButton.find());
    await user.click(ui.blankRadio.get());

    await user.click(ui.languageSelect.get());
    await user.click(byRole('option', { name: 'C' }).get());

    expect(ui.importerA.get()).toBeInTheDocument();
    expect(ui.importerB.get()).toBeInTheDocument();

    await user.click(ui.copyRadio.get());
    expect(ui.importerA.query()).not.toBeInTheDocument();
    expect(ui.importerB.query()).not.toBeInTheDocument();

    await user.click(ui.extendRadio.get());
    expect(ui.importerA.query()).not.toBeInTheDocument();
    expect(ui.importerB.query()).not.toBeInTheDocument();
  });
});

it('should be able to restore a quality profile', async () => {
  const user = userEvent.setup();
  serviceMock.setAdmin();
  renderQualityProfiles();

  await user.click(await ui.restoreButton.find());
  expect(await ui.restoreProfileDialog.find()).toBeInTheDocument();

  const profileXMLBackup = new File(['c-sonarsource-06756'], 'c-sonarsource-06756', {
    type: 'application/xml',
  });
  const input = screen.getByLabelText<HTMLInputElement>(/backup/);
  // Very hacky way to upload the file in the test
  input.removeAttribute('required');

  await userEvent.upload(input, profileXMLBackup);
  expect(input.files?.item(0)).toStrictEqual(profileXMLBackup);

  await user.click(ui.restoreProfileDialog.byRole('button', { name: 'restore' }).get());

  expect(await screen.findByText(/quality_profiles.restore_profile.success/)).toBeInTheDocument();
});

it('should be able to compare profiles', async () => {
  // From the list page
  const user = userEvent.setup();
  serviceMock.setAdmin();
  renderQualityProfiles();

  // For language with 1 profle we should not see compare action
  await user.click(await ui.listProfileActions('c quality profile', 'C').find());
  expect(ui.compareButton.query()).not.toBeInTheDocument();

  await user.click(ui.listProfileActions('java quality profile', 'Java').get());
  expect(ui.compareButton.get()).toBeInTheDocument();
  await user.click(ui.compareButton.get());
  expect(ui.compareDropdown.get()).toBeInTheDocument();
  expect(ui.profileActions('java quality profile', 'Java').query()).not.toBeInTheDocument();
  expect(ui.changelogLink.query()).not.toBeInTheDocument();

  await user.click(ui.compareDropdown.get());
  await user.click(byRole('option', { name: 'java quality profile #2' }).get());

  expect(await ui.comparisonDiffTableHeading(1, 'java quality profile').find()).toBeInTheDocument();
  expect(ui.comparisonDiffTableHeading(1, 'java quality profile #2').get()).toBeInTheDocument();
  expect(ui.comparisonModifiedTableHeading(1).get()).toBeInTheDocument();

  expect(
    ui.comparisonModifiedTableHeading(1).byLabelText('severity_impact.BLOCKER').get(),
  ).toBeInTheDocument();
  expect(
    ui.comparisonModifiedTableHeading(1).byLabelText('severity_impact.LOW').get(),
  ).toBeInTheDocument();

  // java quality profile is not editable
  expect(ui.activeRuleButton('java quality profile').query()).not.toBeInTheDocument();
  expect(ui.deactivateRuleButton('java quality profile').query()).not.toBeInTheDocument();
});

it('should be able to compare profiles without impacts', async () => {
  // From the list page
  const user = userEvent.setup();
  serviceMock.comparisonResult = mockCompareResult({
    modified: [
      {
        impacts: [],
        key: 'java:S1698',
        name: '== and != should not be used when equals is overridden',
        left: {
          params: {},
          severity: 'MINOR',
        },
        right: {
          params: {},
          severity: 'CRITICAL',
        },
      },
    ],
  });
  serviceMock.setAdmin();
  renderQualityProfiles();

  await user.click(await ui.listProfileActions('java quality profile', 'Java').find());
  expect(ui.compareButton.get()).toBeInTheDocument();
  await user.click(ui.compareButton.get());
  await user.click(ui.compareDropdown.get());
  await user.click(byRole('option', { name: 'java quality profile #2' }).get());

  expect(await ui.comparisonModifiedTableHeading(1).find()).toBeInTheDocument();

  expect(
    ui
      .comparisonModifiedTableHeading(1)
      .byLabelText(/severity_impact/)
      .query(),
  ).not.toBeInTheDocument();
});

it('should be able to activate or deactivate rules in comparison page', async () => {
  // From the list page
  const user = userEvent.setup();
  serviceMock.setAdmin();
  renderQualityProfiles();

  await user.click(await ui.listProfileActions('java quality profile #2', 'Java').find());
  await user.click(ui.compareButton.get());

  await user.click(ui.compareDropdown.get());
  await user.click(byRole('option', { name: 'java quality profile' }).get());

  expect(await ui.summaryFewerRules(1).find()).toBeInTheDocument();
  expect(ui.summaryAdditionalRules(1).get()).toBeInTheDocument();

  // Activate
  await user.click(ui.activeRuleButton('java quality profile #2').get());
  expect(ui.popup.get()).toBeInTheDocument();

  await user.click(ui.activateConfirmButton.get());
  expect(ui.summaryFewerRules(1).query()).not.toBeInTheDocument();

  // Deactivate
  await user.click(await ui.deactivateRuleButton('java quality profile #2').find());
  expect(ui.confirmationModal.get()).toBeInTheDocument();
  await user.click(ui.deactivateConfirmButton.get());
  expect(ui.summaryAdditionalRules(1).query()).not.toBeInTheDocument();
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
