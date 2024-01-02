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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from '../../helpers/testUtils';
import { TreeMap, TreeMapProps } from '../TreeMap';

it('should render correctly and forward click event', async () => {
  const user = userEvent.setup();
  const items = [
    {
      key: '1',
      size: 10,
      color: '#777',
      label: 'SonarQube_Server',
    },
    {
      key: '2',
      size: 30,
      color: '#777',
      label: 'SonarQube_Web',
      sourceData: 123,
    },
    {
      key: '3',
      size: 20,
      gradient: '#777',
      label: 'SonarQube_Search',
    },
  ];
  const onRectangleClick = jest.fn();
  const { container } = renderTreeMap({
    items,
    onRectangleClick,
  });

  expect(container).toMatchSnapshot();

  await user.click(screen.getByRole('link', { name: 'SonarQube_Web' }));
  expect(onRectangleClick).toHaveBeenCalledTimes(1);
  expect(onRectangleClick).toHaveBeenCalledWith(items[1]);
});

function renderTreeMap(props: Partial<TreeMapProps<unknown>>) {
  return render(
    <TreeMap height={100} items={[]} onRectangleClick={jest.fn()} width={100} {...props} />,
  );
}
