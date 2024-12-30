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

import { ContentCell, Table, TableRow } from '~design-system';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { Cve } from '../../types/cves';
import DateFormatter from '../intl/DateFormatter';

type Props = {
  cve: Cve;
};

export function CveDetails({ cve }: Readonly<Props>) {
  const { id, description, cwes, cvssScore, epssScore, epssPercentile, publishedAt } = cve;
  return (
    <>
      <h2>{id}</h2>
      <p>{description}</p>
      <Table columnCount={2} aria-label={translate('rule.cve_details')}>
        {cwes.length > 0 && (
          <TableRow>
            <ContentCell>{translate('rule.cve_details.cwes')}</ContentCell>
            <ContentCell>{cwes.join(', ')}</ContentCell>
          </TableRow>
        )}
        <TableRow>
          <ContentCell>{translate('rule.cve_details.epss_score')}</ContentCell>
          <ContentCell>
            {translateWithParameters(
              'rule.cve_details.epss_score.value',
              Math.round(epssScore * 100),
              Math.round(epssPercentile * 100),
            )}
          </ContentCell>
        </TableRow>
        {typeof cvssScore === 'number' && (
          <TableRow>
            <ContentCell>{translate('rule.cve_details.cvss_score')}</ContentCell>
            <ContentCell>{cvssScore.toFixed(1)}</ContentCell>
          </TableRow>
        )}
        <TableRow>
          <ContentCell>{translate('rule.cve_details.published_date')}</ContentCell>
          <ContentCell>
            <DateFormatter date={publishedAt} />
          </ContentCell>
        </TableRow>
      </Table>
    </>
  );
}
