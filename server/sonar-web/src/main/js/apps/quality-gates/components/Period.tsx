/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import Checkbox from '../../../components/controls/Checkbox';
import { Metric } from '../../../app/types';
import { isDiffMetric } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';

interface Props {
  canEdit: boolean;
  metric: Metric;
  onPeriodChange?: (checked: boolean) => void;
  period: boolean;
}

export default class Period extends React.PureComponent<Props> {
  renderPeriodValue() {
    const { metric, period } = this.props;
    const isRating = metric.type === 'RATING';

    if (isDiffMetric(metric.key)) {
      return (
        <span className="note">{translate('quality_gates.condition.leak.unconditional')}</span>
      );
    }

    if (isRating) {
      return <span className="note">{translate('quality_gates.condition.leak.never')}</span>;
    }

    return period
      ? translate('quality_gates.condition.leak.yes')
      : translate('quality_gates.condition.leak.no');
  }

  render() {
    const { canEdit, metric, onPeriodChange, period } = this.props;
    const isRating = metric && metric.type === 'RATING';

    if (isRating || isDiffMetric(metric.key) || !canEdit) {
      return this.renderPeriodValue();
    }

    return <Checkbox checked={period} onCheck={onPeriodChange || (() => {})} />;
  }
}
