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

import { groupBy, sortBy } from 'lodash';
import * as React from 'react';
import { Location, withRouter } from '../../../components/hoc/withRouter';
import { SafeHTMLInjection, SanitizeLevel } from '../../../helpers/sanitize';
import { SettingDefinitionAndValue } from '../../../types/settings';
import { Component } from '../../../types/types';
import { getSubCategoryDescription, getSubCategoryName } from '../utils';
import DefinitionsList from './DefinitionsList';
import EmailForm from './EmailForm';

export interface SubCategoryDefinitionsListProps {
  category: string;
  component?: Component;
  location: Location;
  settings: Array<SettingDefinitionAndValue>;
  subCategory?: string;
  displaySubCategoryTitle?: boolean;
  onUpdate?: () => void;
}

export class SubCategoryDefinitionsList extends React.PureComponent<SubCategoryDefinitionsListProps> {
  componentDidUpdate(prevProps: SubCategoryDefinitionsListProps) {
    const { hash } = this.props.location;
    if (hash && prevProps.location.hash !== hash) {
      const query = `[data-key=${hash.substring(1).replace(/[.#/]/g, '\\$&')}]`;
      const element = document.querySelector<HTMLHeadingElement | HTMLLIElement>(query);
      this.scrollToSubCategoryOrDefinition(element);
    }
  }

  scrollToSubCategoryOrDefinition = (element: HTMLHeadingElement | HTMLLIElement | null) => {
    if (element) {
      const { hash } = this.props.location;
      if (hash && hash.substring(1) === element.getAttribute('data-key')) {
        element.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' });
      }
    }
  };

  renderEmailForm = (subCategoryKey: string) => {
    const isEmailSettings = this.props.category === 'general' && subCategoryKey === 'email';
    if (!isEmailSettings) {
      return null;
    }
    return <EmailForm />;
  };

  render() {
    const { displaySubCategoryTitle = true, settings, subCategory, component } = this.props;
    const bySubCategory = groupBy(settings, (setting) => setting.definition.subCategory);
    const subCategories = Object.keys(bySubCategory).map((key) => ({
      key,
      name: getSubCategoryName(bySubCategory[key][0].definition.category, key),
      description: getSubCategoryDescription(bySubCategory[key][0].definition.category, key),
    }));
    const sortedSubCategories = sortBy(subCategories, (subCategory) =>
      subCategory.name.toLowerCase()
    );
    const filteredSubCategories = subCategory
      ? sortedSubCategories.filter((c) => c.key === subCategory)
      : sortedSubCategories;
    return (
      <ul className="settings-sub-categories-list">
        {filteredSubCategories.map((subCategory) => (
          <li key={subCategory.key}>
            {displaySubCategoryTitle && (
              <h2
                className="settings-sub-category-name"
                data-key={subCategory.key}
                ref={this.scrollToSubCategoryOrDefinition}
              >
                {subCategory.name}
              </h2>
            )}

            {subCategory.description != null && (
              <SafeHTMLInjection
                htmlAsString={subCategory.description}
                sanitizeLevel={SanitizeLevel.RESTRICTED}
              >
                <div className="settings-sub-category-description markdown" />
              </SafeHTMLInjection>
            )}

            <DefinitionsList
              component={component}
              scrollToDefinition={this.scrollToSubCategoryOrDefinition}
              settings={bySubCategory[subCategory.key]}
              onUpdate={this.props.onUpdate}
            />
            {this.renderEmailForm(subCategory.key)}
          </li>
        ))}
      </ul>
    );
  }
}

export default withRouter(SubCategoryDefinitionsList);
