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
import DocLink from '../../../components/common/DocLink';
import { translate } from '../../../helpers/l10n';

export function HotspotDisabledFilterTooltip() {
  return (
    <div className="sw-body-sm sw-w-[190px]">
      <p>
        {translate('indexation.page_unavailable.description')}{' '}
        {translate('indexation.filter_unavailable.description')}
      </p>
      <hr className="sw-mx-0 sw-my-3 sw-p-0 sw-w-full" />
      <span className="sw-body-sm-highlight">{translate('indexation.learn_more')}</span>
      <DocLink
        className="sw-ml-1"
        onMouseDown={(e) => {
          // This tooltip content is rendered in the context of a <Dropdown>, and <DropdownToggler>
          // captures the "focus out" event and closes the dropdown, preventing us from clicking
          // this link. We preventDefault() to avoid this behavior.
          e.preventDefault();
        }}
        to="/instance-administration/reindexing/"
      >
        {translate('indexation.reindexing')}
      </DocLink>
    </div>
  );
}
