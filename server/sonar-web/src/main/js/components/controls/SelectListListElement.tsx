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
import { Checkbox, ListItem } from '~design-system';

interface Props {
  disabled?: boolean;
  element: string;
  onSelect: (element: string) => Promise<void>;
  onUnselect: (element: string) => Promise<void>;
  renderElement: (element: string) => React.ReactNode | [React.ReactNode, React.ReactNode];
  selected: boolean;
}

export default function SelectListListElement(props: Readonly<Props>) {
  const { disabled, element, onSelect, onUnselect, renderElement, selected } = props;

  const [loading, setLoading] = React.useState(false);

  const handleCheck = React.useCallback(
    (checked: boolean) => {
      setLoading(true);
      const request = checked ? onSelect : onUnselect;
      request(element)
        .then(() => setLoading(false))
        .catch(() => setLoading(false));
    },
    [element, setLoading, onSelect, onUnselect],
  );

  let item = renderElement(element);
  let extra;
  if (Array.isArray(item)) {
    extra = item[1];
    item = item[0];
  }
  return (
    <ListItem className="sw-flex sw-justify-between">
      <Checkbox checked={selected} disabled={disabled} loading={loading} onCheck={handleCheck}>
        <span className="sw-ml-4">{item}</span>
      </Checkbox>
      {extra && <span>{extra}</span>}
    </ListItem>
  );
}
