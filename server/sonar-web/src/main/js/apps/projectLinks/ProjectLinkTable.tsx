/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import { ActionCell, ContentCell, Note, Table, TableRow } from 'design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import { orderLinks } from '../../helpers/projectLinks';
import { ProjectLink } from '../../types/types';
import LinkRow from './ProjectLinkRow';

interface Props {
  links: ProjectLink[];
  onDelete: (linkId: string) => Promise<void>;
}

export default function ProjectLinkTable({ links, onDelete }: Readonly<Props>) {
  if (!links.length) {
    return <Note>{translate('project_links.no_results')}</Note>;
  }

  const orderedLinks = orderLinks(links);

  const linkRows = orderedLinks.map((link) => (
    <LinkRow key={link.id} link={link} onDelete={onDelete} />
  ));

  const header = (
    <TableRow>
      <ContentCell>{translate('project_links.name')}</ContentCell>
      <ContentCell>{translate('project_links.url')}</ContentCell>
      <ActionCell>&nbsp;</ActionCell>
    </TableRow>
  );

  return (
    <Table columnCount={3} header={header}>
      {linkRows}
    </Table>
  );
}
