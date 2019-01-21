/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import PluginChangeLog from './PluginChangeLog';
import Dropdown from '../../../components/controls/Dropdown';
import EllipsisIcon from '../../../components/icons-components/EllipsisIcon';
import { ButtonLink } from '../../../components/ui/buttons';
import { Release, Update } from '../../../api/plugins';

interface Props {
  release: Release;
  update: Update;
}

export default function PluginChangeLogButton({ release, update }: Props) {
  return (
    <Dropdown
      className="display-inline-block little-spacer-left"
      overlay={<PluginChangeLog release={release} update={update} />}>
      <ButtonLink className="js-changelog issue-rule">
        <EllipsisIcon />
      </ButtonLink>
    </Dropdown>
  );
}
