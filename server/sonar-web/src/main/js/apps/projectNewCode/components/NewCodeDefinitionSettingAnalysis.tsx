/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { subDays } from 'date-fns';
import { SelectionCard } from 'design-system';
import * as React from 'react';
import { useEffect, useState } from 'react';
import { getProjectActivity } from '../../../api/projectActivity';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import NewCodeDefinitionAnalysisWarning from '../../../components/new-code-definition/NewCodeDefinitionAnalysisWarning';
import { parseDate, toShortISO8601String } from '../../../helpers/dates';
import { translate } from '../../../helpers/l10n';
import { NewCodeDefinitionType } from '../../../types/new-code-definition';
import { Analysis } from '../../../types/project-activity';

export interface Props {
  onSelect: (selection: NewCodeDefinitionType) => void;
  selected: boolean;
  analysis: string;
  branch: string;
  component: string;
}

export default function NewCodeDefinitionSettingAnalysis({
  onSelect,
  selected,
  analysis,
  branch,
  component,
}: Readonly<Props>) {
  const [parsedAnalysis, setParsedAnalysis] = useState<Analysis>();

  const BASE_DAY_SEARCH = 30;

  useEffect(() => {
    async function fetchAnalyses(range = BASE_DAY_SEARCH, initial = true) {
      const result = await getProjectActivity({
        branch,
        project: component,
        from: range ? toShortISO8601String(subDays(new Date(), range)) : undefined,
      });
      // If the selected analysis wasn't found in the default 30 days range, redo the search
      if (initial && analysis && !result.analyses.find((a) => a.key === analysis)) {
        fetchAnalyses(0, false);
        return;
      }

      const filteredResult = result.analyses.find((a) => a.key === analysis);
      setParsedAnalysis(filteredResult);
    }

    fetchAnalyses();
  }, [analysis, branch, component]);

  const parsedDate = parsedAnalysis?.date ? parseDate(parsedAnalysis?.date) : '';

  return (
    <SelectionCard
      disabled
      onClick={() => onSelect(NewCodeDefinitionType.SpecificAnalysis)}
      selected={selected}
      title={translate('baseline.specific_analysis')}
    >
      <p className="sw-mb-4">{translate('baseline.specific_analysis.description')}</p>
      {parsedAnalysis && (
        <p className="sw-mb-4">
          <span>
            {`${translate('baseline.specific_analysis')}: `}
            {parsedDate ? <DateTimeFormatter date={parsedDate} /> : '?'}
          </span>
        </p>
      )}

      <NewCodeDefinitionAnalysisWarning />
    </SelectionCard>
  );
}
