/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { RawHotspot } from '../../../types/security-hotspots';
import { getStatusOptionFromStatusAndResolution } from '../utils';

export interface HotspotListItemProps {
  hotspot: RawHotspot;
  onClick: (hotspot: RawHotspot) => void;
  selected: boolean;
}

export default function HotspotListItem(props: HotspotListItemProps) {
  const { hotspot, selected } = props;
  return (
    <a
      className={classNames('hotspot-item', { highlight: selected })}
      href="#"
      onClick={() => !selected && props.onClick(hotspot)}>
      <div className="little-spacer-left">{hotspot.message}</div>
      <div className="badge spacer-top">
        {translate(
          'hotspots.status_option',
          getStatusOptionFromStatusAndResolution(hotspot.status, hotspot.resolution)
        )}
      </div>
    </a>
  );
}
