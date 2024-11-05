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

import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { IssueChangelogDiff as TypeIssueChangelogDiff } from '../../../types/types';

export interface IssueChangelogDiffProps {
  diff: TypeIssueChangelogDiff;
}

export default function IssueChangelogDiff({ diff }: Readonly<IssueChangelogDiffProps>) {
  const diffComputedValues = {
    newValue: diff.newValue ?? '',
    oldValue: diff.oldValue ?? '',
  };

  if (diff.key === 'file') {
    return (
      <p>
        {translateWithParameters(
          'issue.change.file_move',
          diffComputedValues.oldValue,
          diffComputedValues.newValue,
        )}
      </p>
    );
  }

  if (['from_long_branch', 'from_branch'].includes(diff.key)) {
    return (
      <p>
        {translateWithParameters(
          'issue.change.from_branch',
          diffComputedValues.oldValue,
          diffComputedValues.newValue,
        )}
      </p>
    );
  }

  if (diff.key === 'from_short_branch') {
    // Applies to both legacy short lived branch and pull request
    return (
      <p>
        {translateWithParameters(
          'issue.change.from_non_branch',
          diffComputedValues.oldValue,
          diffComputedValues.newValue,
        )}
      </p>
    );
  }

  if (diff.key === 'line') {
    return (
      <p>
        {translateWithParameters('issue.changelog.line_removed_X', diffComputedValues.oldValue)}
      </p>
    );
  }

  if (diff.key === 'impactSeverity') {
    const [softwareQuality, newSeverity] = diffComputedValues.newValue.split(':');
    const [_, oldSeverity] = diffComputedValues.oldValue.split(':');
    return (
      <p>
        {translateWithParameters(
          'issue.changelog.impactSeverity',
          softwareQuality,
          newSeverity,
          oldSeverity,
        )}
      </p>
    );
  }

  if (diff.key === 'effort') {
    diffComputedValues.newValue = formatMeasure(diff.newValue, 'WORK_DUR');
    diffComputedValues.oldValue = formatMeasure(diff.oldValue, 'WORK_DUR');
  }

  let message =
    diff.newValue !== undefined
      ? translateWithParameters(
          'issue.changelog.changed_to',
          translate('issue.changelog.field', diff.key),
          diffComputedValues.newValue,
        )
      : translateWithParameters(
          'issue.changelog.removed',
          translate('issue.changelog.field', diff.key),
        );

  if (diff.oldValue !== undefined) {
    message += ` (${translateWithParameters('issue.changelog.was', diffComputedValues.oldValue)})`;
  }

  return <p>{message}</p>;
}
