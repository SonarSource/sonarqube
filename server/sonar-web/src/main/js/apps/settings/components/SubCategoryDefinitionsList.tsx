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
import { BasicSeparator, Note, SubTitle } from 'design-system';
import { groupBy, sortBy } from 'lodash';
import * as React from 'react';
import { Location, withRouter } from '../../../components/hoc/withRouter';
import { sanitizeStringRestricted } from '../../../helpers/sanitize';
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
}

class SubCategoryDefinitionsList extends React.PureComponent<SubCategoryDefinitionsListProps> {
  componentDidUpdate(prevProps: SubCategoryDefinitionsListProps) {
    const { hash } = this.props.location;
    if (hash && prevProps.location.hash !== hash) {
      const query = `[data-scroll-key=${hash.substring(1).replace(/[.#/]/g, '\\$&')}]`;
      const element = document.querySelector<HTMLHeadingElement | HTMLLIElement>(query);
      this.scrollToSubCategoryOrDefinition(element);
    }
  }

  scrollToSubCategoryOrDefinition = (element: HTMLHeadingElement | HTMLLIElement | null) => {
    if (element) {
      const { hash } = this.props.location;
      if (hash && hash.substring(1) === element.getAttribute('data-scroll-key')) {
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
      subCategory.name.toLowerCase(),
    );
    const filteredSubCategories = subCategory
      ? sortedSubCategories.filter((c) => c.key === subCategory)
      : sortedSubCategories;
    return (
      <ul>
        {filteredSubCategories.map((subCategory, index) => (
          <li className="sw-p-6" key={subCategory.key}>
            {displaySubCategoryTitle && (
              <SubTitle
                as="h3"
                data-key={subCategory.key}
                ref={this.scrollToSubCategoryOrDefinition}
              >
                {subCategory.name}
              </SubTitle>
            )}
            {subCategory.description != null && (
              <Note
                className="markdown"
                // eslint-disable-next-line react/no-danger
                dangerouslySetInnerHTML={{
                  __html: sanitizeStringRestricted(subCategory.description),
                }}
              />
            )}
            <BasicSeparator className="sw-mt-6" />
            <DefinitionsList
              component={component}
              scrollToDefinition={this.scrollToSubCategoryOrDefinition}
              settings={bySubCategory[subCategory.key]}
            />
            {this.renderEmailForm(subCategory.key)}

            {
              // Add a separator to all but the last element
              index !== filteredSubCategories.length - 1 && <BasicSeparator />
            }
          </li>
        ))}
      </ul>
    );
  }
}

export default withRouter(SubCategoryDefinitionsList);
