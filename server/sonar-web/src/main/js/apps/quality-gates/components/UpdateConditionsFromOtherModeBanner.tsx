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

import { Button, ButtonVariety, IconRefresh } from '@sonarsource/echoes-react';
import { FormattedMessage, useIntl } from 'react-intl';
import { CardWithPrimaryBackground, SubHeadingHighlight } from '~design-system';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { DocLink } from '../../../helpers/doc-links';
import { useStandardExperienceModeQuery } from '../../../queries/mode';
import { Condition } from '../../../types/types';
import UpdateConditionsFromOtherModeModal from './UpdateConditionsFromOtherModeModal';

interface Props {
  newCodeConditions: Condition[];
  overallCodeConditions: Condition[];
  qualityGateName: string;
}

export default function UpdateConditionsFromOtherModeBanner(props: Readonly<Props>) {
  const { data: isStandard } = useStandardExperienceModeQuery();
  const intl = useIntl();
  return (
    <CardWithPrimaryBackground className="sw-mt-9 sw-p-8">
      <SubHeadingHighlight className="sw-mb-2">
        {intl.formatMessage(
          { id: 'quality_gates.mode_banner.title' },
          {
            mode: intl.formatMessage({
              id: `settings.mode.${isStandard ? 'standard' : 'mqr'}.name`,
            }),
          },
        )}
      </SubHeadingHighlight>
      <div>
        <FormattedMessage
          id="quality_gates.mode_banner.description"
          values={{
            link: (
              <DocumentationLink to={isStandard ? DocLink.ModeStandard : DocLink.ModeMQR}>
                {intl.formatMessage({
                  id: `settings.mode.${isStandard ? 'standard' : 'mqr'}.name`,
                })}
              </DocumentationLink>
            ),
            otherMode: intl.formatMessage({
              id: `settings.mode.${isStandard ? 'mqr' : 'standard'}.name`,
            }),
          }}
        />
      </div>
      <UpdateConditionsFromOtherModeModal {...props}>
        <Button className="sw-mt-4" prefix={<IconRefresh />} variety={ButtonVariety.Primary}>
          {intl.formatMessage({ id: 'quality_gates.mode_banner.button' })}
        </Button>
      </UpdateConditionsFromOtherModeModal>
    </CardWithPrimaryBackground>
  );
}
