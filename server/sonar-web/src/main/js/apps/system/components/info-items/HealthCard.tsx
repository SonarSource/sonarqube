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
import { map } from 'lodash';
import HealthItem from './HealthItem';
import Section from './Section';
import BoxedGroupAccordion from '../../../../components/controls/BoxedGroupAccordion';
import { HealthType, SysValueObject } from '../../../../api/system';
import { LOGS_LEVELS, groupSections, getLogsLevel } from '../../utils';
import { translate } from '../../../../helpers/l10n';
import { Alert } from '../../../../components/ui/Alert';

interface Props {
  biggerHealth?: boolean;
  health?: HealthType;
  healthCauses?: string[];
  onClick: (toggledCard: string) => void;
  open: boolean;
  name: string;
  sysInfoData: SysValueObject;
}

export default function HealthCard({
  biggerHealth,
  health,
  healthCauses,
  onClick,
  open,
  name,
  sysInfoData
}: Props) {
  const { mainSection, sections } = groupSections(sysInfoData);
  const showFields = open && mainSection && Object.keys(mainSection).length > 0;
  const showSections = open && sections;
  const logLevel = getLogsLevel(sysInfoData);
  const showLogLevelWarning = logLevel && logLevel !== LOGS_LEVELS[0];
  return (
    <BoxedGroupAccordion
      data={name}
      onClick={onClick}
      open={open}
      renderHeader={() => (
        <>
          {showLogLevelWarning && (
            <Alert
              className="boxed-group-accordion-alert spacer-left"
              display="inline"
              variant="warning">
              {translate('system.log_level.warning.short')}
            </Alert>
          )}
          {health && (
            <HealthItem
              biggerHealth={biggerHealth}
              className="pull-right"
              health={health}
              healthCauses={healthCauses}
              name={name}
            />
          )}
        </>
      )}
      title={name}>
      {showFields && <Section items={mainSection} />}
      {showSections &&
        map(sections, (section, name) => <Section items={section} key={name} name={name} />)}
    </BoxedGroupAccordion>
  );
}
