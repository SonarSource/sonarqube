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

import styled from '@emotion/styled';
import { Heading, LinkHighlight, Text } from '@sonarsource/echoes-react';
import { useIntl } from 'react-intl';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { DocLink } from '../../../helpers/doc-links';
import { translate } from '../../../helpers/l10n';
import { AdditionalCategoryComponentProps } from './AdditionalCategories';
import CategoryDefinitionsList from './CategoryDefinitionsList';

export function AnalysisScope(props: AdditionalCategoryComponentProps) {
  const { component, definitions, selectedCategory } = props;
  const intl = useIntl();

  return (
    <>
      <StyledGrid className="sw-pt-1 sw-px-1 sw-gap-2">
        <Heading as="h2" className="sw-col-span-2">
          {translate('property.category.exclusions')}
        </Heading>

        <Text className="sw-col-span-2 sw-mt-4 sw-mb-3">
          {intl.formatMessage(
            { id: 'settings.analysis_scope.introduction' },
            {
              link: (text) => (
                <DocumentationLink
                  shouldOpenInNewTab
                  highlight={LinkHighlight.CurrentColor}
                  to={DocLink.AnalysisScopeWildcardPatterns}
                >
                  {text}
                </DocumentationLink>
              ),
            },
          )}
        </Text>

        <Text as="p" className="sw-col-span-2">
          {intl.formatMessage(
            { id: 'settings.analysis_scope.wildcards.introduction' },
            {
              link: (text) => (
                <DocumentationLink
                  shouldOpenInNewTab
                  highlight={LinkHighlight.CurrentColor}
                  to={DocLink.AnalysisScope}
                >
                  {text}
                </DocumentationLink>
              ),
            },
          )}
        </Text>

        <Text>*</Text>
        <Text isSubdued>{translate('settings.analysis_scope.wildcards.zero_more_char')}</Text>

        <Text>**</Text>
        <Text isSubdued>{translate('settings.analysis_scope.wildcards.zero_more_dir')}</Text>

        <Text>?</Text>
        <Text isSubdued>{translate('settings.analysis_scope.wildcards.single_char')}</Text>
      </StyledGrid>

      <CategoryDefinitionsList
        category={selectedCategory}
        component={component}
        definitions={definitions}
      />
    </>
  );
}

const StyledGrid = styled.div`
  display: grid;
  grid-template-columns: 1.5rem auto;
`;
