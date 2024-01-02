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
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { AlmKeys } from '../../../types/alm-settings';
import AllSet from './AllSet';
import Step from './Step';

export interface AllSetStepProps {
  alm: AlmKeys;
  open: boolean;
  stepNumber: number;
  willRefreshAutomatically?: boolean;
}

export default function AllSetStep(props: AllSetStepProps) {
  const { alm, open, stepNumber, willRefreshAutomatically } = props;
  return (
    <Step
      finished={false}
      open={open}
      renderForm={() => (
        <div className="boxed-group-inner">
          <AllSet alm={alm} willRefreshAutomatically={willRefreshAutomatically} />
        </div>
      )}
      stepNumber={stepNumber}
      stepTitle={translate('onboarding.tutorial.ci_outro.all_set.title')}
    />
  );
}
