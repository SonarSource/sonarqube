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

import { Heading } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { groupBy, sortBy } from 'lodash';
import * as React from 'react';
import { BasicSeparator, Note, SafeHTMLInjection, SanitizeLevel } from '~design-system';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Location } from '~sonar-aligned/types/router';
import { SettingDefinitionAndValue } from '../../../types/settings';
import { Component } from '../../../types/types';
import { SUB_CATEGORY_EXCLUSIONS } from '../constants';
import { getSubCategoryDescription, getSubCategoryName } from '../utils';
import DefinitionsList from './DefinitionsList';

export interface SubCategoryDefinitionsListProps {
  category: string;
  component?: Component;
  displaySubCategoryTitle?: boolean;
  location: Location;
  noPadding?: boolean;
  settings: Array<SettingDefinitionAndValue>;
  subCategory?: string;
}

class SubCategoryDefinitionsList extends React.PureComponent<SubCategoryDefinitionsListProps> {
  componentDidUpdate(prevProps: SubCategoryDefinitionsListProps) {
    const { hash } = this.props.location;
    if (hash.length > 0 && prevProps.location.hash !== hash) {
      const query = `[data-scroll-key=${hash.substring(1).replace(/[.#/]/g, '\\$&')}]`;
      const element = document.querySelector<HTMLHeadingElement | HTMLLIElement>(query);
      this.scrollToSubCategoryOrDefinition(element);
    }
  }

  scrollToSubCategoryOrDefinition = (element: HTMLHeadingElement | HTMLLIElement | null) => {
    if (element) {
      const { hash } = this.props.location;
      if (hash.length > 0 && hash.substring(1) === element.getAttribute('data-scroll-key')) {
        element.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' });
      }
    }
  };

  render() {
    const {
      category,
      displaySubCategoryTitle = true,
      settings,
      subCategory,
      component,
      noPadding,
    } = this.props;
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
      : sortedSubCategories.filter((c) => !SUB_CATEGORY_EXCLUSIONS[category]?.includes(c.key));

    return (
      <ul className={classNames({ 'sw-mx-6': !noPadding })}>
        {filteredSubCategories.map((subCategory, index) => (
          <li className={classNames({ 'sw-py-6': !noPadding })} key={subCategory.key}>
            {displaySubCategoryTitle && (
              <Heading
                as="h3"
                data-key={subCategory.key}
                ref={this.scrollToSubCategoryOrDefinition}
              >
                {subCategory.name}
              </Heading>
            )}

            {subCategory.description != null && (
              <SafeHTMLInjection
                htmlAsString={subCategory.description}
                sanitizeLevel={SanitizeLevel.RESTRICTED}
              >
                <Note className="markdown" />
              </SafeHTMLInjection>
            )}

            <BasicSeparator className="sw-mt-6" />
            <DefinitionsList
              component={component}
              scrollToDefinition={this.scrollToSubCategoryOrDefinition}
              settings={bySubCategory[subCategory.key]}
            />
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
