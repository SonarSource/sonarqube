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
import * as React from 'react';
import DocLink from '../../../components/common/DocLink';
import { translate } from '../../../helpers/l10n';
import { AdditionalCategoryComponentProps } from './AdditionalCategories';
import CategoryDefinitionsList from './CategoryDefinitionsList';

export function AnalysisScope(props: AdditionalCategoryComponentProps) {
  const { component, definitions, selectedCategory } = props;

  return (
    <>
      <p className="spacer-bottom">
        {translate('settings.analysis_scope.wildcards.introduction')}
        <DocLink className="spacer-left" to="/project-administration/narrowing-the-focus/">
          {translate('learn_more')}
        </DocLink>
      </p>

      <table className="data spacer-bottom">
        <tbody>
          <tr>
            <td>*</td>
            <td>{translate('settings.analysis_scope.wildcards.zero_more_char')}</td>
          </tr>
          <tr>
            <td>**</td>
            <td>{translate('settings.analysis_scope.wildcards.zero_more_dir')}</td>
          </tr>
          <tr>
            <td>?</td>
            <td>{translate('settings.analysis_scope.wildcards.single_char')}</td>
          </tr>
        </tbody>
      </table>

      <div className="settings-sub-category">
        <CategoryDefinitionsList
          category={selectedCategory}
          component={component}
          definitions={definitions}
        />
      </div>
    </>
  );
}
