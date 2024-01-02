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
import { PopupPlacement, Tags, Tooltip } from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../helpers/l10n';
import './TagsList.css';

interface Props {
  allowUpdate?: boolean;
  className?: string;
  tags: string[];
  overlay?: React.ReactNode;
  tagsClassName?: string;
  tagsToDisplay?: number;
}

export default function TagsList({
  allowUpdate = false,
  className,
  tags,
  overlay,
  tagsClassName,
  tagsToDisplay = 2,
}: Readonly<Props>) {
  const [open, setOpen] = React.useState(false);

  return (
    <Tags
      allowUpdate={allowUpdate}
      ariaTagsListLabel={translateWithParameters('tags_list_x', tags.join(', '))}
      className={className}
      emptyText={translate('no_tags')}
      menuId="rule-tags-menu"
      onClose={() => setOpen(false)}
      open={open}
      overlay={overlay}
      popupPlacement={PopupPlacement.Bottom}
      tags={tags}
      tagsClassName={tagsClassName}
      tagsToDisplay={tagsToDisplay}
      tooltip={Tooltip}
    />
  );
}
