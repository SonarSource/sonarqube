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
import * as React from 'react';
import { isProvided, getLinkName } from '../../project-admin/links/utils';
import { ProjectLink } from '../../../app/types';
import BugTrackerIcon from '../../../components/ui/BugTrackerIcon';

interface Props {
  link: ProjectLink;
}

export default function MetaLink({ link }: Props) {
  return (
    <li>
      <a className="link-with-icon" href={link.url} rel="nofollow" target="_blank">
        <MetaLinkIcon link={link} /> {getLinkName(link)}
      </a>
    </li>
  );
}

function MetaLinkIcon({ link }: Props) {
  if (link.type === 'issue') {
    return <BugTrackerIcon />;
  }

  return isProvided(link) ? (
    <i className={`icon-color-link icon-${link.type}`} />
  ) : (
    <i className="icon-color-link icon-detach" />
  );
}
