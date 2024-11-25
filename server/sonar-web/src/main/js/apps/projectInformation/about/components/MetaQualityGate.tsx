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
import { Link, Note, StyledMutedText, SubHeading } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../../helpers/l10n';
import { getQualityGateUrl } from '../../../../helpers/urls';

interface Props {
  isAiAssured?: boolean;
  qualityGate: { isDefault?: boolean; name: string };
  organization: string;
}

export default function MetaQualityGate({ organization, qualityGate, isAiAssured }: Props) {
  return (
    <div>
      <SubHeading id="quality-gate-header">{translate('project.info.quality_gate')}</SubHeading>

      <ul className="sw-flex sw-flex-col sw-gap-2" aria-labelledby="quality-gate-header">
        <li>
          {qualityGate.isDefault && <Note className="sw-mr-2">({translate('default')})</Note>}
          <Link to={getQualityGateUrl(organization, qualityGate.name)}>{qualityGate.name}</Link>
        </li>
      </ul>
      {isAiAssured === true && (
        <StyledMutedText className="sw-text-wrap sw-mt-2">
          <FormattedMessage id="project.info.quality_gate.ai_code_assurance.description" />
        </StyledMutedText>
      )}
    </div>
  );
}
