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
import { keyBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { getValues } from '../../../api/settings';
import DocLink from '../../../components/common/DocLink';
import { Alert } from '../../../components/ui/Alert';
import { translate } from '../../../helpers/l10n';
import {
  ExtendedSettingDefinition,
  SettingDefinitionAndValue,
  SettingValue,
} from '../../../types/settings';
import { Component } from '../../../types/types';
import SubCategoryDefinitionsList from './SubCategoryDefinitionsList';

interface Props {
  category: string;
  component?: Component;
  definitions: ExtendedSettingDefinition[];
  subCategory?: string;
  displaySubCategoryTitle?: boolean;
}

interface State {
  settings: SettingDefinitionAndValue[];
  displaySecurityWarning: boolean;
}

export default class CategoryDefinitionsList extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { settings: [], displaySecurityWarning: false };

  componentDidMount() {
    this.mounted = true;

    this.loadSettingValues();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.category !== this.props.category) {
      this.loadSettingValues();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  shouldDisplaySecurityWarning = (settings: SettingDefinitionAndValue[]) => {
    const { category, subCategory } = this.props;
    if (category !== 'authentication' || subCategory === 'saml') {
      return false;
    }
    const isEnabled = settings.find(
      (s) => s.definition.key === `sonar.auth.${subCategory}.enabled`
    );
    const isAllowUsersToSignUpEnabled = settings.find(
      (s) => s.definition.key === `sonar.auth.${subCategory}.allowUsersToSignUp`
    );
    let organizationsSetting;
    if (subCategory === 'github') {
      organizationsSetting = settings.find(
        (s) => s.definition.key === `sonar.auth.${subCategory}.organizations`
      );
    }
    if (subCategory === 'bitbucket') {
      organizationsSetting = settings.find(
        (s) => s.definition.key === `sonar.auth.${subCategory}.workspaces`
      );
    }
    if (subCategory === 'gitlab') {
      organizationsSetting = settings.find(
        (s) => s.definition.key === `sonar.auth.${subCategory}.allowedGroups`
      );
    }

    if (
      isEnabled?.settingValue?.value === 'true' &&
      isAllowUsersToSignUpEnabled?.settingValue?.value === 'true' &&
      organizationsSetting?.settingValue === undefined
    ) {
      return true;
    }
    return false;
  };

  loadSettingValues = async () => {
    const { category, component, definitions } = this.props;

    const categoryDefinitions = definitions.filter(
      (definition) => definition.category.toLowerCase() === category.toLowerCase()
    );

    const keys = categoryDefinitions.map((definition) => definition.key);

    const values: SettingValue[] = await getValues({
      keys,
      component: component?.key,
    }).catch(() => []);
    const valuesByDefinitionKey = keyBy(values, 'key');

    const settings: SettingDefinitionAndValue[] = categoryDefinitions.map((definition) => {
      const settingValue = valuesByDefinitionKey[definition.key];
      return {
        definition,
        settingValue,
      };
    });

    const displaySecurityWarning = this.shouldDisplaySecurityWarning(settings);

    this.setState({ settings, displaySecurityWarning });
  };

  render() {
    const { category, component, subCategory, displaySubCategoryTitle } = this.props;
    const { settings, displaySecurityWarning } = this.state;

    return (
      <>
        {displaySecurityWarning && (
          <Alert variant="error">
            <FormattedMessage
              id={`settings.authentication.${subCategory}.organization.warning`}
              defaultMessage={translate(
                `settings.authentication.${subCategory}.organization.warning`
              )}
              values={{
                learn_more: (
                  <DocLink
                    to={`/instance-administration/authentication/${
                      subCategory === 'bitbucket' ? 'bitbucket-cloud' : subCategory
                    }/#setting-your-authentication-settings-in-sonarqube`}
                  >
                    {translate('learn_more')}
                  </DocLink>
                ),
              }}
            />
          </Alert>
        )}
        <SubCategoryDefinitionsList
          category={category}
          component={component}
          settings={settings}
          subCategory={subCategory}
          displaySubCategoryTitle={displaySubCategoryTitle}
          onUpdate={this.loadSettingValues}
        />
      </>
    );
  }
}
