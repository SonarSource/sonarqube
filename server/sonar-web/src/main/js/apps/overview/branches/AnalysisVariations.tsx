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
import { FormattedMessage } from 'react-intl';
import { TrendDirection, TrendIcon, TrendType, themeColor } from '~design-system';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { MetricType } from '~sonar-aligned/types/metrics';
import { AnalysisMeasuresVariations } from '../../../types/project-activity';

interface AnalysisVariationsProps {
  isFirstAnalysis?: boolean;
  variations: AnalysisMeasuresVariations;
}

interface VariationProps {
  isGoodIfGrowing: boolean;
  label: string;
  showVariationIcon?: boolean;
  valueType?: MetricType;
  variation?: number;
}

function Variation(props: Readonly<VariationProps>) {
  const {
    isGoodIfGrowing,
    label,
    showVariationIcon = true,
    valueType = MetricType.Integer,
    variation,
  } = props;

  if (variation === undefined) {
    return null;
  }

  const formattedValue = formatMeasure(variation, valueType);

  if (!showVariationIcon) {
    return (
      <span className="sw-flex sw-items-center sw-mx-2">
        {formattedValue} {<FormattedMessage id={label} />}
      </span>
    );
  }

  let trendIconDirection = TrendDirection.Equal;
  let trendIconType = TrendType.Neutral;
  if (variation !== 0) {
    trendIconDirection = variation > 0 ? TrendDirection.Up : TrendDirection.Down;
    trendIconType = variation > 0 === isGoodIfGrowing ? TrendType.Positive : TrendType.Negative;
  }
  const variationIcon = <TrendIcon direction={trendIconDirection} type={trendIconType} />;

  const variationToDisplay = formattedValue.startsWith('-') ? formattedValue : `+${formattedValue}`;

  return (
    <span className="sw-flex sw-items-center sw-mx-1">
      {variationIcon} {variationToDisplay} {<FormattedMessage id={label} />}
    </span>
  );
}

export function AnalysisVariations(props: Readonly<AnalysisVariationsProps>) {
  const { isFirstAnalysis, variations } = props;

  const issuesVariation = variations.violations ?? 0;
  const coverageVariation = variations.coverage;
  const duplicationsVariation = variations.duplicated_lines_density;

  return (
    <div className="sw-flex sw-items-center sw-mt-1">
      <FormattedMessage
        id={
          isFirstAnalysis
            ? 'overview.activity.variations.first_analysis'
            : 'overview.activity.variations.new_analysis'
        }
      />
      <Variation
        isGoodIfGrowing={false}
        label="project_activity.graphs.issues"
        showVariationIcon={!isFirstAnalysis}
        variation={issuesVariation}
      />
      {coverageVariation !== undefined && <SeparatorContainer>&bull;</SeparatorContainer>}
      <Variation
        isGoodIfGrowing
        label="project_activity.graphs.coverage"
        showVariationIcon={!isFirstAnalysis}
        valueType={MetricType.Percent}
        variation={coverageVariation}
      />
      {duplicationsVariation !== undefined && <SeparatorContainer>&bull;</SeparatorContainer>}
      <Variation
        isGoodIfGrowing={false}
        label="project_activity.graphs.duplications"
        showVariationIcon={!isFirstAnalysis}
        valueType={MetricType.Percent}
        variation={duplicationsVariation}
      />
    </div>
  );
}

const SeparatorContainer = styled.span`
  color: ${themeColor('iconStatus')};
`;
