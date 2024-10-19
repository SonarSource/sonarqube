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
import { ButtonIcon, IconInfo, Popover } from '@sonarsource/echoes-react';
import { ScaleTime } from 'd3-scale';
import * as React from 'react';
import { shouldShowSplitLine } from '../../helpers/activity-graph';
import { DocLink } from '../../helpers/doc-links';
import { translate } from '../../helpers/l10n';
import DocumentationLink from '../common/DocumentationLink';

interface Props {
  paddingLeft: number;
  splitPointDate?: Date;
  xScale: ScaleTime<number, number>;
}

export default function SplitLinePopover({ paddingLeft, splitPointDate, xScale }: Readonly<Props>) {
  const [popoverOpen, setPopoverOpen] = React.useState(false);
  const showSplitLine = shouldShowSplitLine(splitPointDate, xScale);

  if (!showSplitLine) {
    return null;
  }

  return (
    <Popover
      isOpen={popoverOpen}
      title={translate('project_activity.graphs.rating_split.title')}
      description={translate('project_activity.graphs.rating_split.description')}
      footer={
        <DocumentationLink standalone to={DocLink.MetricDefinitions}>
          {translate('learn_more')}
        </DocumentationLink>
      }
    >
      <ButtonIcon
        isIconFilled
        style={{ left: `${Math.round(xScale(splitPointDate)) + paddingLeft}px` }}
        className="sw-border-none sw-absolute sw-bg-transparent sw--top-3 sw--translate-x-2/4"
        ariaLabel={translate('project_activity.graphs.rating_split.info_icon')}
        Icon={IconInfo}
        onClick={() => setPopoverOpen(!popoverOpen)}
      />
    </Popover>
  );
}
