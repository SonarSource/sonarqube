/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import React from 'react';
import classNames from 'classnames';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';

/*::
type Props = {|
  className?: string,
  onClick: () => void
|};
*/

/* eslint-disable max-len */
const icon = (
  <svg width="21" height="24" viewBox="0 0 21 24">
    <path d="M3.845 12.9992l5.993 5.993.052.056c.049.061.093.122.129.191.082.159.121.339.111.518-.006.102-.028.203-.064.298-.149.39-.537.652-.954.644-.102-.002-.204-.019-.301-.052-.148-.05-.273-.135-.387-.241l-8.407-8.407 8.407-8.407.056-.052c.061-.048.121-.092.19-.128.116-.06.237-.091.366-.108.076-.004.075-.004.153-.003.155.015.3.052.437.129.088.051.169.115.239.19.246.266.33.656.214.999-.051.149-.135.273-.241.387l-5.983 5.984c5.287-.044 10.577-.206 15.859.013.073.009.091.009.163.027.187.047.359.15.49.292.075.081.136.175.18.276.044.101.072.209.081.319.032.391-.175.775-.521.962-.097.052-.202.089-.311.107-.073.012-.091.01-.165.013H3.845z" />
  </svg>
);
/* eslint-enable max-len */

export default function BackButton(props /*: Props */) {
  const handleClick = (event /*: Event */) => {
    event.preventDefault();
    props.onClick();
  };

  return (
    <Tooltip overlay={translate('issues.return_to_list')}>
      <a
        className={classNames('concise-issues-list-header-button', props.className)}
        href="#"
        onClick={handleClick}>
        {icon}
      </a>
    </Tooltip>
  );
}
