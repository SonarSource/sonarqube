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

import { ContentCell, NumericalCell, Table, TableRow, Title } from '~design-system';
import { translate } from '../../../helpers/l10n';
import { Condition as ConditionType, Dict, Metric, QualityGate } from '../../../types/types';
import Condition from './Condition';

interface Props {
  canEdit: boolean;
  conditions: ConditionType[];
  isCaycModal?: boolean;
  metrics: Dict<Metric>;
  qualityGate: QualityGate;
  scope: 'new' | 'overall' | 'new-cayc';
  showEdit?: boolean;
}

function Header() {
  return (
    <TableRow>
      <ContentCell>
        <Title className="sw-typo-semibold sw-m-0 sw-whitespace-nowrap">
          {translate('quality_gates.conditions.metric')}
        </Title>
      </ContentCell>
      <ContentCell>
        <Title className="sw-typo-semibold sw-m-0 sw-whitespace-nowrap">
          {translate('quality_gates.conditions.operator')}
        </Title>
      </ContentCell>
      <NumericalCell>
        <Title className="sw-typo-semibold sw-m-0 sw-whitespace-nowrap">
          {translate('quality_gates.conditions.value')}
        </Title>
      </NumericalCell>
      <ContentCell />
    </TableRow>
  );
}

export default function ConditionsTable({
  qualityGate,
  metrics,
  canEdit,
  scope,
  conditions,
  isCaycModal,
  showEdit,
}: Readonly<Props>) {
  return (
    <Table
      columnCount={4}
      columnWidths={['300px', 'auto', 'auto', 'auto']}
      className="sw-my-2"
      header={<Header />}
      data-test={`quality-gates__conditions-${scope}`}
      data-testid={`quality-gates__conditions-${scope}`}
    >
      {conditions.map((condition) => (
        <Condition
          canEdit={canEdit}
          condition={condition}
          key={condition.id}
          metric={metrics[condition.metric]}
          qualityGate={qualityGate}
          isCaycModal={isCaycModal}
          showEdit={showEdit}
        />
      ))}
    </Table>
  );
}
