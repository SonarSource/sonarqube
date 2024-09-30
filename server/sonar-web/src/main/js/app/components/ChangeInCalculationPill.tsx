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
import { Popover } from '@sonarsource/echoes-react';
import { Pill, PillVariant } from 'design-system';
import * as React from 'react';
import DocumentationLink from '../../components/common/DocumentationLink';
import { DocLink } from '../../helpers/doc-links';
import { translate } from '../../helpers/l10n';
import { useIsLegacyCCTMode } from '../../queries/settings';
import { ComponentQualifier } from '../../sonar-aligned/types/component';

interface Props {
  qualifier: ComponentQualifier;
}

export default function ChangeInCalculation({ qualifier }: Readonly<Props>) {
  const [isPopoverOpen, setIsPopoverOpen] = React.useState(false);
  const { data: isLegacy, isLoading } = useIsLegacyCCTMode();

  if (isLegacy || isLoading) {
    return null;
  }

  return (
    <Popover
      isOpen={isPopoverOpen}
      title={translate('projects.awaiting_scan.title')}
      description={translate(`projects.awaiting_scan.description.${qualifier}`)}
      footer={
        <DocumentationLink to={DocLink.CleanCodeIntroduction}>
          {translate('learn_more')}
        </DocumentationLink>
      }
    >
      <Pill
        variant={PillVariant.Info}
        className="sw-ml-2"
        onClick={() => setIsPopoverOpen(!isPopoverOpen)}
      >
        {translate('projects.awaiting_scan')}
      </Pill>
    </Popover>
  );
}
