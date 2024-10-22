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

import { Display } from '@sonarsource/echoes-react';
import { Note, QualityGateIndicator } from '~design-system';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { translate } from '../../../helpers/l10n';
import { Status } from '../../../sonar-aligned/types/common';

interface Props {
  status?: Status;
}

export default function QualityGateStatus(props: Readonly<Props>) {
  const { status = 'NONE' } = props;

  return (
    <div className="sw-flex sw-gap-3" data-spotlight-id="cayc-promotion-3">
      <QualityGateIndicator size="xl" status={status} />
      <div className="sw-flex sw-flex-col sw-ml-2 sw-justify-around">
        <div className="sw-flex sw-items-center">
          <Note as="h1">{translate('overview.quality_gate')}</Note>
          <HelpTooltip
            className="sw-ml-2"
            overlay={<div>{translate('overview.quality_gate.help')}</div>}
          />
        </div>
        <Display>{translate('metric.level', status === 'NONE' ? 'NOT_COMPUTED' : status)}</Display>
      </div>
    </div>
  );
}
