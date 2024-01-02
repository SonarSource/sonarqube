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
import userEvent from '@testing-library/user-event';
import { addGlobalSuccessMessage } from 'design-system';
import selectEvent from 'react-select-event';
import {
  ProfileProject,
  associateProject,
  getProfileProjects,
  searchQualityProfiles,
} from '../../../api/quality-profiles';
import handleRequiredAuthorization from '../../../app/utils/handleRequiredAuthorization';
import { mockComponent } from '../../../helpers/mocks/component';
import {
  RenderContext,
  renderAppWithComponentContext,
} from '../../../helpers/testReactTestingUtils';
import { byLabelText, byRole, byText } from '../../../helpers/testSelector';
import { Component } from '../../../types/types';
import routes from '../routes';

jest.mock('../../../api/quality-profiles', () => {
  const { mockQualityProfile } = jest.requireActual('../../../helpers/testMocks');

  return {
    associateProject: jest.fn().mockResolvedValue({}),
    dissociateProject: jest.fn().mockResolvedValue({}),
    searchQualityProfiles: jest.fn().mockResolvedValue({
      profiles: [
        mockQualityProfile({
          key: 'css',
          language: 'css',
          name: 'css profile',
          languageName: 'CSS',
        }),
        mockQualityProfile({
          key: 'java',
          language: 'java',
          name: 'java profile',
          languageName: 'Java',
        }),
        mockQualityProfile({
          key: 'js',
          language: 'js',
          name: 'js profile',
          languageName: 'JavaScript',
        }),
        mockQualityProfile({
          key: 'ts',
          language: 'ts',
          isDefault: true,
          name: 'ts profile',
          languageName: 'Typescript',
        }),
        mockQualityProfile({
          key: 'html',
          language: 'html',
          name: 'html profile',
          languageName: 'HTML',
        }),
        mockQualityProfile({
          key: 'html_default',
          language: 'html',
          isDefault: true,
          isBuiltIn: true,
          name: 'html default profile',
          languageName: 'HTML',
        }),
      ],
    }),
    getProfileProjects: jest.fn(({ key }) => {
      const results: ProfileProject[] = [];
      if (key === 'css' || key === 'java' || key === 'js' || key === 'ts' || key === 'java') {
        results.push({
          key: 'my-project',
          name: 'My project',
          selected: true,
        });
      }
      return Promise.resolve({ results });
    }),
  };
});

jest.mock('design-system', () => ({
  ...jest.requireActual('design-system'),
  addGlobalSuccessMessage: jest.fn(),
}));

jest.mock('../../../app/utils/handleRequiredAuthorization', () => jest.fn());

beforeEach(jest.clearAllMocks);

const ui = {
  pageTitle: byText('project_quality_profiles.page'),
  pageSubTitle: byText('project_quality_profile.subtitle'),
  pageDescription: byText('project_quality_profiles.page.description'),
  helpTooltip: byLabelText('help-tooltip'),
  profileRows: byRole('row'),
  addLanguageButton: byRole('button', { name: 'project_quality_profile.add_language.action' }),
  modalAddLanguageTitle: byText('project_quality_profile.add_language_modal.title'),
  selectLanguage: byRole('combobox', {
    name: 'project_quality_profile.add_language_modal.choose_language',
  }),
  selectProfile: byRole('combobox', {
    name: 'project_quality_profile.add_language_modal.choose_profile',
  }),
  selectUseSpecificProfile: byRole('combobox', {
    name: 'project_quality_profile.always_use_specific',
  }),
  buttonSave: byRole('button', { name: 'save' }),
  buttonChangeProfile: byRole('button', { name: 'project_quality_profile.change_profile' }),
  htmlLanguage: byText('HTML'),
  htmlProfile: byText('html profile'),
  cssLanguage: byText('CSS'),
  cssProfile: byText('css profile'),
  htmlDefaultProfile: byText('html default profile'),
  htmlActiveRuleslink: byRole('link', { name: '10' }),
  radioButtonUseInstanceDefault: byRole('radio', {
    name: /project_quality_profile.always_use_default/,
  }),
  radioButtonUseSpecific: byRole('radio', { name: /project_quality_profile.always_use_specific/ }),
  newAnalysisWarningMessage: byText('project_quality_profile.requires_new_analysis'),
  builtInTag: byText('quality_profiles.built_in'),
};

it('should be able to add and change profile for languages', async () => {
  const user = userEvent.setup();
  renderProjectQualityProfilesApp({
    languages: {
      css: { key: 'css', name: 'CSS' },
      ts: { key: 'ts', name: 'TS' },
      js: { key: 'js', name: 'JS' },
      java: { key: 'java', name: 'JAVA' },
      html: { key: 'html', name: 'HTML' },
    },
  });

  expect(ui.pageTitle.get()).toBeInTheDocument();
  expect(ui.pageDescription.get()).toBeInTheDocument();
  expect(await ui.addLanguageButton.find()).toBeInTheDocument();
  await expect(ui.helpTooltip.get()).toHaveATooltipWithContent(
    'quality_profiles.list.projects.help',
  );
  expect(ui.profileRows.getAll()).toHaveLength(5);
  expect(ui.cssLanguage.get()).toBeInTheDocument();
  expect(ui.cssProfile.get()).toBeInTheDocument();

  await user.click(ui.addLanguageButton.get());

  // Opens the add language modal
  expect(ui.modalAddLanguageTitle.get()).toBeInTheDocument();
  expect(ui.selectLanguage.get()).toBeEnabled();
  expect(ui.selectProfile.get()).toBeDisabled();
  expect(ui.buttonSave.get()).toBeInTheDocument();

  await selectEvent.select(ui.selectLanguage.get(), 'HTML');
  expect(ui.selectProfile.get()).toBeEnabled();
  await selectEvent.select(ui.selectProfile.get(), 'html profile');
  await user.click(ui.buttonSave.get());
  expect(associateProject).toHaveBeenLastCalledWith(
    expect.objectContaining({ key: 'html', name: 'html profile' }),
    'my-project',
  );
  expect(addGlobalSuccessMessage).toHaveBeenCalledWith(
    'project_quality_profile.successfully_updated.HTML',
  );

  // Updates the page after API call
  const htmlRow = byRole('row', {
    name: 'HTML html profile 10',
  });

  expect(ui.htmlLanguage.get()).toBeInTheDocument();
  expect(ui.htmlProfile.get()).toBeInTheDocument();
  expect(ui.profileRows.getAll()).toHaveLength(6);
  expect(htmlRow.get()).toBeInTheDocument();
  expect(htmlRow.byRole('link', { name: '10' }).get()).toHaveAttribute(
    'href',
    '/coding_rules?activation=true&qprofile=html',
  );
  expect(ui.builtInTag.query()).not.toBeInTheDocument();

  await user.click(
    htmlRow.byRole('button', { name: 'project_quality_profile.change_profile' }).get(),
  );

  //Opens modal to change profile
  expect(ui.radioButtonUseInstanceDefault.get()).not.toBeChecked();
  expect(ui.radioButtonUseSpecific.get()).toBeChecked();
  expect(ui.newAnalysisWarningMessage.get()).toBeInTheDocument();
  expect(ui.selectUseSpecificProfile.get()).toBeInTheDocument();

  await selectEvent.select(ui.selectUseSpecificProfile.get(), 'html default profile');
  await user.click(ui.buttonSave.get());

  expect(addGlobalSuccessMessage).toHaveBeenCalledWith(
    'project_quality_profile.successfully_updated.HTML',
  );

  // Updates the page after API call
  expect(ui.htmlProfile.query()).not.toBeInTheDocument();
  expect(ui.htmlDefaultProfile.get()).toBeInTheDocument();
  expect(ui.builtInTag.get()).toBeInTheDocument();
});

it('should call authorization api when permissions is not proper', () => {
  renderProjectQualityProfilesApp({}, { configuration: { showQualityProfiles: false } });
  expect(handleRequiredAuthorization).toHaveBeenCalled();
});

it('should still show page with add language button when api fails', async () => {
  jest.mocked(searchQualityProfiles).mockRejectedValueOnce(null);
  jest.mocked(getProfileProjects).mockRejectedValueOnce(null);

  renderProjectQualityProfilesApp();
  expect(ui.pageTitle.get()).toBeInTheDocument();
  expect(ui.pageDescription.get()).toBeInTheDocument();
  expect(await ui.addLanguageButton.find()).toBeInTheDocument();
});

function renderProjectQualityProfilesApp(
  context?: RenderContext,
  componentOverrides: Partial<Component> = { configuration: { showQualityProfiles: true } },
) {
  return renderAppWithComponentContext('project/quality_profiles', routes, context, {
    component: mockComponent(componentOverrides),
  });
}
