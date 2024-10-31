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

import { Button, ButtonVariety, IconQuestionMark, Popover } from '@sonarsource/echoes-react';
import { useIntl } from 'react-intl';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { DocLink } from '../../../helpers/doc-links';
import { useStandardExperienceMode } from '../../../queries/settings';

export default function QGMetricsMismatchHelp() {
  const intl = useIntl();
  const { data: isStandardMode } = useStandardExperienceMode();
  return (
    <Popover
      title={intl.formatMessage({ id: 'issues.qg_mismatch.title' })}
      description={intl.formatMessage({ id: 'issues.qg_mismatch.description' }, { isStandardMode })}
      footer={
        <DocumentationLink standalone to={DocLink.QualityGates}>
          {intl.formatMessage({ id: 'issues.qg_mismatch.link' })}
        </DocumentationLink>
      }
    >
      <Button
        className="sw-p-0 sw-h-fit sw-min-h-fit"
        aria-label={intl.formatMessage({ id: 'help' })}
        variety={ButtonVariety.DefaultGhost}
      >
        <IconQuestionMark />
      </Button>
    </Popover>
  );
}
