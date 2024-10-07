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
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import QualityProfilesServiceMock from '../../../api/mocks/QualityProfilesServiceMock';
import { renderAppRoutes } from '../../../helpers/testReactTestingUtils';
import routes from '../routes';

jest.mock('../../../api/quality-profiles');
jest.mock('../../../api/rules');

beforeEach(() => {
  serviceMock.reset();
});

const serviceMock = new QualityProfilesServiceMock();
const ui = {
  loading: byRole('status', { name: 'loading' }),
  permissionSection: byRole('region', { name: 'permissions.page' }),
  projectSection: byRole('region', { name: 'projects' }),
  rulesSection: byRole('region', { name: 'rules' }),
  exportersSection: byRole('region', { name: 'quality_profiles.exporters' }),
  inheritanceSection: byRole('region', { name: 'quality_profiles.profile_inheritance' }),
  grantPermissionButton: byRole('button', {
    name: 'quality_profiles.grant_permissions_to_more_users',
  }),
  dialog: byRole('dialog'),
  selectField: byRole('combobox'),
  selectUserOrGroup: byRole('combobox', { name: 'quality_profiles.search_description' }),
  twitterCheckbox: byRole('checkbox', { name: 'Twitter Twitter' }),
  benflixCheckbox: byRole('checkbox', { name: 'Benflix Benflix' }),
  addButton: byRole('button', { name: 'add_verb' }),
  removeButton: byRole('button', { name: 'remove' }),
  closeButton: byRole('button', { name: 'close' }),
  changeProjectsButton: byRole('button', { name: 'quality_profiles.change_projects' }),
  changeButton: byRole('button', { name: 'change_verb' }),
  withoutFilterButton: byRole('radio', { name: 'quality_gates.projects.without' }),
  changeParentButton: byRole('button', { name: 'quality_profiles.change_parent' }),
  qualityProfileActions: byRole('button', {
    name: /quality_profiles.actions/,
  }),
  qualityProfilesHeader: byRole('heading', { name: 'quality_profiles.page' }),
  deleteQualityProfileButton: byRole('menuitem', { name: 'delete' }),
  activateMoreRulesButton: byRole('button', { name: 'quality_profiles.activate_more' }),
  activateMoreLink: byRole('link', { name: 'quality_profiles.activate_more' }),
  activateMoreRulesLink: byRole('menuitem', { name: 'quality_profiles.activate_more_rules' }),
  backUpLink: byRole('menuitem', { name: 'backup_verb open_in_new_tab' }),
  compareLink: byRole('menuitem', { name: 'compare' }),
  extendButton: byRole('menuitem', { name: 'extend' }),
  copyButton: byRole('menuitem', { name: 'copy' }),
  renameButton: byRole('menuitem', { name: 'rename' }),
  setAsDefaultButton: byRole('menuitem', { name: 'set_as_default' }),
  newNameInput: byRole('textbox', { name: /quality_profiles.new_name/ }),
  qualityProfilePageLink: byRole('link', { name: 'quality_profiles.back_to_list' }),
  rulesConsistencyRow: byRole('row', { name: /rule.clean_code_attribute_category.CONSISTENT/ }),
  rulesSecurityRow: byRole('row', { name: /rule.clean_code_attribute_category.SECURITY/ }),
  rulesMissingSonarWayWarning: byText('quality_profiles.sonarway_missing_rules_description'),
  rulesMissingSonarWayLink: byRole('link', {
    name: /quality_profiles.sonarway_see_x_missing_rules/,
  }),
  rulesDeprecatedWarning: byText('quality_profiles.deprecated_rules_description'),
  rulesDeprecatedLink: byRole('link', { name: '8' }),

  waitForDataLoaded: async () => {
    await waitFor(() => {
      expect(ui.loading.query()).not.toBeInTheDocument();
    });
  },

  checkRuleRow: (name: string, active: number, inactive: number) => {
    expect(
      byRole('row', { name: new RegExp(`${name}.+${active}.+${inactive}`) }).get(),
    ).toBeInTheDocument();
  },
};

describe('Admin or user with permission', () => {
  beforeEach(() => {
    serviceMock.setAdmin();
  });

  describe('Permissions', () => {
    it('should be able to grant permission to a user and remove it', async () => {
      const user = userEvent.setup();
      renderQualityProfile();
      await ui.waitForDataLoaded();

      expect(await ui.permissionSection.find()).toBeInTheDocument();

      // Add user
      await user.click(ui.grantPermissionButton.get());
      expect(ui.dialog.get()).toBeInTheDocument();

      await user.click(ui.selectUserOrGroup.get());
      await user.click(byRole('option', { name: /^Buzz/ }).get());

      await user.click(ui.addButton.get());
      expect(ui.permissionSection.byText('Buzz').get()).toBeInTheDocument();

      // Remove User
      await user.click(
        ui.permissionSection
          .byRole('button', { name: 'quality_profiles.permissions.remove.user_x.Buzz' })
          .get(),
      );
      expect(ui.dialog.get()).toBeInTheDocument();
      await user.click(ui.removeButton.get());
      expect(ui.permissionSection.byText('buzz').query()).not.toBeInTheDocument();
    });

    it('should be able to grant permission to a group and remove it', async () => {
      const user = userEvent.setup();
      renderQualityProfile();
      await ui.waitForDataLoaded();

      expect(await ui.permissionSection.find()).toBeInTheDocument();

      // Add Group
      await user.click(ui.grantPermissionButton.get());
      expect(ui.dialog.get()).toBeInTheDocument();

      await user.click(ui.selectUserOrGroup.get());
      await user.click(byRole('option', { name: /^ACDC/ }).get());

      await user.click(ui.addButton.get());
      expect(ui.permissionSection.byText('ACDC').get()).toBeInTheDocument();

      // Remove group
      await user.click(
        ui.permissionSection
          .byRole('button', { name: 'quality_profiles.permissions.remove.group_x.ACDC' })
          .get(),
      );
      expect(ui.dialog.get()).toBeInTheDocument();
      await user.click(ui.removeButton.get());
      expect(ui.permissionSection.byText('ACDC').query()).not.toBeInTheDocument();
    });

    it('should not be able to grant permission if the profile is built-in', async () => {
      renderQualityProfile('sonar');
      await ui.waitForDataLoaded();
      expect(screen.getByRole('heading', { name: /\bSonar way\b/ })).toBeInTheDocument();
      expect(ui.permissionSection.query()).not.toBeInTheDocument();
    });
  });

  describe('Projects', () => {
    it('should be able to add a project to Quality Profile with active rules', async () => {
      const user = userEvent.setup();
      renderQualityProfile();
      await ui.waitForDataLoaded();

      expect(await ui.projectSection.find()).toBeInTheDocument();

      expect(ui.projectSection.byText('Twitter').query()).not.toBeInTheDocument();
      await user.click(ui.changeProjectsButton.get());
      expect(ui.dialog.get()).toBeInTheDocument();

      await user.click(ui.withoutFilterButton.get());
      await user.click(ui.twitterCheckbox.get());
      await user.click(ui.closeButton.get());
      expect(ui.projectSection.byText('Twitter').get()).toBeInTheDocument();
    });

    it('should be able to remove a project from a Quality Profile with active rules', async () => {
      const user = userEvent.setup();
      renderQualityProfile();
      await ui.waitForDataLoaded();

      expect(await ui.projectSection.find()).toBeInTheDocument();

      expect(ui.projectSection.byText('Benflix').get()).toBeInTheDocument();
      await user.click(ui.changeProjectsButton.get());
      expect(ui.dialog.get()).toBeInTheDocument();

      await user.click(ui.benflixCheckbox.get());
      await user.click(ui.closeButton.get());
      expect(ui.projectSection.byText('Benflix').query()).not.toBeInTheDocument();
    });

    it('should not be able to change project for Quality Profile with no active rules', async () => {
      renderQualityProfile('no-rule-qp');
      await ui.waitForDataLoaded();

      expect(await ui.projectSection.find()).toBeInTheDocument();

      expect(ui.changeProjectsButton.get()).toHaveAttribute('disabled');
    });

    it('should not be able to change projects for default profiles', async () => {
      renderQualityProfile('sonar');
      await ui.waitForDataLoaded();

      expect(await ui.projectSection.find()).toBeInTheDocument();

      expect(
        ui.projectSection.byText('quality_profiles.projects_for_default').get(),
      ).toBeInTheDocument();
    });
  });

  describe('Rules', () => {
    it('should be able to activate more rules', async () => {
      renderQualityProfile();
      await ui.waitForDataLoaded();

      expect(await ui.rulesSection.find()).toBeInTheDocument();

      expect(ui.activateMoreLink.get()).toBeInTheDocument();
      expect(ui.activateMoreLink.get()).toHaveAttribute(
        'href',
        '/coding_rules?qprofile=old-php-qp&activation=false',
      );
    });

    it("shouldn't be able to activate more rules for built in Quality Profile", async () => {
      renderQualityProfile('sonar');
      await ui.waitForDataLoaded();
      expect(await ui.rulesSection.find()).toBeInTheDocument();
      expect(ui.activateMoreRulesButton.get()).toBeInTheDocument();
      expect(ui.activateMoreRulesButton.get()).toBeDisabled();
    });
  });

  describe('Inheritance', () => {
    it("should be able to change a quality profile's parents", async () => {
      const user = userEvent.setup();
      renderQualityProfile();
      await ui.waitForDataLoaded();

      expect(await ui.inheritanceSection.find()).toBeInTheDocument();

      // Parents
      expect(ui.inheritanceSection.byText('PHP Sonar way 1').get()).toBeInTheDocument();
      expect(ui.inheritanceSection.byText('PHP Sonar way 2').query()).not.toBeInTheDocument();
      // Children
      expect(ui.inheritanceSection.byText('PHP way').get()).toBeInTheDocument();

      await user.click(ui.changeParentButton.get());
      expect(await ui.dialog.find()).toBeInTheDocument();
      expect(ui.changeButton.get()).toBeDisabled();

      await user.click(ui.selectField.get());
      await user.click(byRole('option', { name: 'PHP Sonar way 2' }).get());

      await user.click(ui.changeButton.get());
      expect(ui.dialog.query()).not.toBeInTheDocument();

      await ui.waitForDataLoaded();

      // Parents
      expect(ui.inheritanceSection.byText('PHP Sonar way 2').get()).toBeInTheDocument();
      expect(ui.inheritanceSection.byText('PHP Sonar way 1').query()).not.toBeInTheDocument();
      // Children
      expect(ui.inheritanceSection.byText('PHP way').get()).toBeInTheDocument();
    });

    it("should not be able to change a Built-in quality profile's parents", async () => {
      renderQualityProfile('php-sonar-way-1');
      await ui.waitForDataLoaded();

      expect(await ui.inheritanceSection.find()).toBeInTheDocument();
      expect(ui.changeParentButton.query()).not.toBeInTheDocument();
    });
  });

  describe('Actions', () => {
    it('should be able to activate more rules', async () => {
      const user = userEvent.setup();
      renderQualityProfile();
      await ui.waitForDataLoaded();

      await user.click(await ui.qualityProfileActions.find());
      expect(ui.activateMoreRulesLink.get()).toBeInTheDocument();
      expect(ui.activateMoreRulesLink.get()).toHaveAttribute(
        'href',
        '/coding_rules?qprofile=old-php-qp&activation=false',
      );
    });

    it('should be able to extend a quality profile', async () => {
      const user = userEvent.setup();
      renderQualityProfile();
      await ui.waitForDataLoaded();

      expect(
        await screen.findByRole('heading', { name: 'Good old PHP quality profile' }),
      ).toBeInTheDocument();

      await user.click(ui.qualityProfileActions.get());
      await user.click(ui.extendButton.get());

      expect(ui.dialog.get()).toBeInTheDocument();
      expect(ui.dialog.byRole('button', { name: 'extend' }).get()).toBeDisabled();

      await user.clear(ui.newNameInput.get());
      await user.type(ui.newNameInput.get(), 'Bad new PHP quality profile');
      await user.click(ui.dialog.byRole('button', { name: 'extend' }).get());

      expect(ui.dialog.query()).not.toBeInTheDocument();

      await ui.waitForDataLoaded();

      expect(screen.getAllByText('Bad new PHP quality profile')).toHaveLength(2);
      expect(screen.getByText('Good old PHP quality profile')).toBeInTheDocument();
    });

    it('should be able to copy a quality profile', async () => {
      const user = userEvent.setup();
      renderQualityProfile();
      await ui.waitForDataLoaded();

      await user.click(await ui.qualityProfileActions.find());
      await user.click(ui.copyButton.get());

      expect(ui.dialog.get()).toBeInTheDocument();
      expect(ui.dialog.byRole('button', { name: 'copy' }).get()).toBeDisabled();

      await user.clear(ui.newNameInput.get());
      await user.type(ui.newNameInput.get(), 'Good old PHP quality profile copy');
      await user.click(ui.dialog.byRole('button', { name: 'copy' }).get());

      expect(ui.dialog.query()).not.toBeInTheDocument();

      await ui.waitForDataLoaded();
      expect(await screen.findAllByText('Good old PHP quality profile copy')).toHaveLength(2);
    });

    it('should be able to rename a quality profile', async () => {
      const user = userEvent.setup();
      renderQualityProfile();
      await ui.waitForDataLoaded();

      await user.click(await ui.qualityProfileActions.find());
      await user.click(ui.renameButton.get());

      expect(ui.dialog.get()).toBeInTheDocument();
      expect(ui.dialog.byRole('button', { name: 'rename' }).get()).toBeDisabled();

      await user.clear(ui.newNameInput.get());
      await user.type(ui.newNameInput.get(), 'Fossil PHP quality profile');
      await user.click(ui.dialog.byRole('button', { name: 'rename' }).get());

      expect(ui.dialog.query()).not.toBeInTheDocument();

      await ui.waitForDataLoaded();
      expect(screen.getAllByText('Fossil PHP quality profile')).toHaveLength(2);
    });

    it('should be able to set a quality profile as default', async () => {
      const user = userEvent.setup();
      renderQualityProfile();
      await ui.waitForDataLoaded();

      await user.click(await ui.qualityProfileActions.find());
      await user.click(ui.setAsDefaultButton.get());

      expect(screen.getAllByText('default')).toHaveLength(2);
    });

    it('should NOT be able to set a quality profile as default if it has no active rules', async () => {
      const user = userEvent.setup();
      renderQualityProfile('no-rule-qp');
      await ui.waitForDataLoaded();

      await user.click(await ui.qualityProfileActions.find());
      expect(ui.setAsDefaultButton.get()).toHaveAttribute('aria-disabled', 'true');
    });

    it("should be able to delete a Quality Profile and it's children", async () => {
      const user = userEvent.setup();
      renderQualityProfile();
      await ui.waitForDataLoaded();

      await user.click(await ui.qualityProfileActions.find());
      await user.click(ui.deleteQualityProfileButton.get());

      expect(ui.dialog.get()).toBeInTheDocument();
      expect(
        ui.dialog
          .byText(/quality_profiles.are_you_sure_want_delete_profile_x_and_descendants/)
          .get(),
      ).toBeInTheDocument();
      await user.click(ui.dialog.byRole('button', { name: 'delete' }).get());

      expect(ui.qualityProfilesHeader.get()).toBeInTheDocument();
      // children
      expect(screen.queryByText('PHP way')).not.toBeInTheDocument();
      expect(screen.queryByText('Good old PHP quality profile')).not.toBeInTheDocument();
    });
  });
});

describe('Users with no permission', () => {
  it('should not be able to activate more rules', async () => {
    renderQualityProfile();
    await ui.waitForDataLoaded();

    expect(await ui.rulesSection.find()).toBeInTheDocument();
    expect(ui.activateMoreLink.query()).not.toBeInTheDocument();
  });

  it('should not be able to grant permission to a user', async () => {
    renderQualityProfile();
    await ui.waitForDataLoaded();

    expect(ui.permissionSection.query()).not.toBeInTheDocument();
  });

  it("should not be able to change a quality profile's parents", async () => {
    renderQualityProfile();
    await ui.waitForDataLoaded();

    expect(await ui.inheritanceSection.find()).toBeInTheDocument();
    expect(ui.inheritanceSection.byText('PHP Sonar way 1').get()).toBeInTheDocument();
    expect(ui.inheritanceSection.byText('PHP way').get()).toBeInTheDocument();

    expect(ui.changeParentButton.query()).not.toBeInTheDocument();
  });

  it('should not be able to change projects for Quality Profile', async () => {
    renderQualityProfile();
    await ui.waitForDataLoaded();

    expect(await ui.projectSection.find()).toBeInTheDocument();
    expect(ui.changeProjectsButton.query()).not.toBeInTheDocument();
  });
});

describe('Every Users', () => {
  it('should be able to see active/inactive rules for a Quality Profile', async () => {
    renderQualityProfile();
    await ui.waitForDataLoaded();

    expect(await ui.rulesSection.find()).toBeInTheDocument();

    ui.checkRuleRow('rule.clean_code_attribute_category.INTENTIONAL', 23, 4);
    ui.checkRuleRow('rule.clean_code_attribute_category.CONSISTENT', 2, 18);
    ui.checkRuleRow('rule.clean_code_attribute_category.ADAPTABLE', 1, 11);
    ui.checkRuleRow('rule.clean_code_attribute_category.RESPONSIBLE', 0, 0);
    ui.checkRuleRow('software_quality.MAINTAINABILITY', 9, 44);
    ui.checkRuleRow('software_quality.RELIABILITY', 16, 1);
    ui.checkRuleRow('software_quality.SECURITY', 0, 14);
  });

  it('should be able to see a warning when some rules are missing compare to Sonar way', async () => {
    renderQualityProfile();
    await ui.waitForDataLoaded();

    expect(await ui.rulesMissingSonarWayWarning.findAll()).toHaveLength(2);
    expect(ui.rulesMissingSonarWayLink.get()).toBeInTheDocument();
    expect(ui.rulesMissingSonarWayLink.get()).toHaveAttribute(
      'href',
      '/coding_rules?qprofile=old-php-qp&activation=false&languages=php',
    );
  });

  it('should be able to see a warning when some rules are deprecated', async () => {
    renderQualityProfile();
    await ui.waitForDataLoaded();

    expect(await ui.rulesDeprecatedWarning.findAll()).toHaveLength(1);
    expect(ui.rulesDeprecatedLink.get()).toBeInTheDocument();
    expect(ui.rulesDeprecatedLink.get()).toHaveAttribute(
      'href',
      '/coding_rules?qprofile=old-php-qp&activation=true&statuses=DEPRECATED',
    );
  });

  it('should be able to see exporters links when there are exporters for the language', async () => {
    renderQualityProfile();
    await ui.waitForDataLoaded();

    expect(await ui.exportersSection.find()).toBeInTheDocument();
    expect(ui.exportersSection.byText('SonarLint for Visual Studio').get()).toBeInTheDocument();
    expect(ui.exportersSection.byText('SonarLint for Eclipse').get()).toBeInTheDocument();
  });

  it('should be informed when the quality profile has not been found', async () => {
    renderQualityProfile('i-dont-exist');
    await ui.waitForDataLoaded();

    expect(
      await screen.findByRole('heading', { name: 'quality_profiles.not_found' }),
    ).toBeInTheDocument();
    expect(ui.qualityProfilePageLink.get()).toBeInTheDocument();
  });

  it('should be able to backup quality profile', async () => {
    const user = userEvent.setup();
    renderQualityProfile();
    await ui.waitForDataLoaded();

    await user.click(await ui.qualityProfileActions.find());
    expect(ui.backUpLink.get()).toHaveAttribute(
      'href',
      '/api/qualityprofiles/backup?language=php&qualityProfile=Good%20old%20PHP%20quality%20profile',
    );
    expect(ui.backUpLink.get()).toHaveAttribute('download', 'old-php-qp.xml');
  });

  it('should not be able to backup a built-in quality profile', async () => {
    const user = userEvent.setup();
    renderQualityProfile('sonar');
    await ui.waitForDataLoaded();

    await user.click(await ui.qualityProfileActions.find());
    expect(ui.backUpLink.query()).not.toBeInTheDocument();
  });

  it('should be able to compare quality profile', async () => {
    const user = userEvent.setup();
    renderQualityProfile();
    await ui.waitForDataLoaded();

    await user.click(await ui.qualityProfileActions.find());

    expect(ui.compareLink.get()).toBeInTheDocument();
    expect(ui.compareLink.get()).toHaveAttribute(
      'href',
      '/profiles/compare?language=php&name=Good+old+PHP+quality+profile',
    );
  });
});

function renderQualityProfile(key = 'old-php-qp') {
  renderAppRoutes(`profiles/show?key=${key}`, routes, {});
}
