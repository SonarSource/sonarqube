/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';

interface Props {
  line: T.SourceLine;
}

export default function LineCoverage({ line }: Props) {
  const className =
    'source-meta source-line-coverage' +
    (line.coverageStatus != null ? ` source-line-${line.coverageStatus}` : '');
  return (
    <td className={className} data-line-number={line.line}>
      <Tooltip overlay={getStatusTooltip(line)} placement="right">
        <div className="source-line-bar" />
      </Tooltip>
    </td>
  );
}

function getStatusTooltip(line: T.SourceLine) {
  if (line.coverageStatus === 'uncovered') {
    if (line.conditions) {
      return translateWithParameters('source_viewer.tooltip.uncovered.conditions', line.conditions);
    } else {
      return translate('source_viewer.tooltip.uncovered');
    }
  } else if (line.coverageStatus === 'covered') {
    if (line.conditions) {
      return translateWithParameters('source_viewer.tooltip.covered.conditions', line.conditions);
    } else {
      return translate('source_viewer.tooltip.covered');
    }
  } else if (line.coverageStatus === 'partially-covered') {
    if (line.conditions) {
      return translateWithParameters(
        'source_viewer.tooltip.partially-covered.conditions',
        line.coveredConditions || 0,
        line.conditions
      );
    } else {
      return translate('source_viewer.tooltip.partially-covered');
    }
  }
  return undefined;
}
