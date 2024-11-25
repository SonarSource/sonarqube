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

import { Heading, LinkStandalone, Text } from '@sonarsource/echoes-react';
import { useIntl } from 'react-intl';
import { translate } from '../../../../helpers/l10n';
import { getQualityGateUrl } from '../../../../helpers/urls';

interface Props {
  qualityGate: { isDefault?: boolean; name: string };
}

export default function MetaQualityGate({ qualityGate }: Props) {
  const intl = useIntl();

  return (
    <section>
      <Heading as="h3">{translate('project.info.quality_gate')}</Heading>
      <ul className="sw-mt-2 sw-flex sw-flex-col sw-gap-3">
        <li>
          {qualityGate.isDefault && (
            <Text isSubdued className="sw-mr-2">
              ({translate('default')})
            </Text>
          )}
          <LinkStandalone
            aria-label={intl.formatMessage(
              { id: 'project.info.quality_gate.link_label' },
              { gate: qualityGate.name },
            )}
            to={getQualityGateUrl(qualityGate.name)}
          >
            {qualityGate.name}
          </LinkStandalone>
        </li>
      </ul>
    </section>
  );
}
