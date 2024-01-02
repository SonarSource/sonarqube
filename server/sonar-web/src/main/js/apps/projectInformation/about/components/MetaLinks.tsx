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
import React from 'react';
import MetaLink from '../../../../components/common/MetaLink';
import { translate } from '../../../../helpers/l10n';
import { orderLinks } from '../../../../helpers/projectLinks';
import { ProjectLink } from '../../../../types/types';

interface Props {
  links: ProjectLink[];
}

export default function MetaLinks({ links }: Props) {
  const orderedLinks = orderLinks(links);

  return (
    <>
      <h3 id="external-links">{translate('overview.external_links')}</h3>
      <ul className="project-info-list" aria-labelledby="external-links">
        {orderedLinks.map((link) => (
          <MetaLink miui key={link.id} link={link} />
        ))}
      </ul>
    </>
  );
}
