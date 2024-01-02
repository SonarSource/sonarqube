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
import * as React from 'react';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import FacetBox, { FacetBoxProps } from '../FacetBox';
import FacetHeader from '../FacetHeader';
import FacetItem from '../FacetItem';
import FacetItemsList, { FacetItemsListProps } from '../FacetItemsList';

it('should render and function correctly', () => {
  const onFacetClick = jest.fn();
  renderFacet(undefined, undefined, undefined, { onClick: onFacetClick });

  // Start closed.
  let facetHeader = screen.getByRole('button', { name: 'foo', expanded: false });
  expect(facetHeader).toBeInTheDocument();
  expect(screen.queryByText('Foo/Bar')).not.toBeInTheDocument();

  // Expand.
  facetHeader.click();
  facetHeader = screen.getByRole('button', { name: 'foo', expanded: true });
  expect(facetHeader).toBeInTheDocument();
  expect(screen.getByText('Foo/Bar')).toBeInTheDocument();

  // Interact with facets.
  const facet1 = screen.getByRole('checkbox', { name: 'Foo/Bar 10' });
  expect(facet1).toHaveClass('active');
  facet1.click();
  expect(onFacetClick).toHaveBeenCalledWith('bar', false);

  const facet2 = screen.getByRole('checkbox', { name: 'Foo/Baz' });
  expect(facet2).not.toHaveClass('active');

  // Collapse again.
  facetHeader.click();
  expect(screen.getByRole('button', { name: 'foo', expanded: false })).toBeInTheDocument();
  expect(screen.queryByText('Foo/Bar')).not.toBeInTheDocument();
});

it('should correctly render a header with helper text', async () => {
  renderFacet(undefined, { helper: 'Help text' });
  await userEvent.tab();
  await userEvent.tab();
  expect(screen.getByText('Help text')).toBeInTheDocument();
});

it('should correctly render a header with value data', () => {
  renderFacet(undefined, { values: ['value 1'] });
  expect(screen.getByText('value 1')).toBeInTheDocument();
  screen.getByRole('button', { name: 'clear_x_filter.foo' }).click();
  expect(screen.queryByText('value 1')).not.toBeInTheDocument();
});

it('should correctly render a disabled header', () => {
  renderFacet(undefined, { onClick: undefined });
  expect(screen.queryByRole('checkbox', { name: 'foo' })).not.toBeInTheDocument();
});

it('should correctly render a facet item list with title', () => {
  renderFacet(undefined, { open: true }, { title: 'My list title' });
  expect(screen.getByText('My list title')).toBeInTheDocument();
});

function renderFacet(
  facetBoxProps: Partial<FacetBoxProps> = {},
  facetHeaderProps: Partial<FacetHeader['props']> = {},
  facetItemListProps: Partial<FacetItemsListProps> = {},
  facetItemProps: Partial<FacetItem['props']> = {}
) {
  function Facet() {
    const [open, setOpen] = React.useState(facetHeaderProps.open ?? false);
    const [values, setValues] = React.useState(facetHeaderProps.values ?? undefined);

    return (
      <FacetBox property="foo" {...facetBoxProps}>
        <FacetHeader
          name="foo"
          onClick={() => setOpen(!open)}
          onClear={() => setValues(undefined)}
          {...{ ...facetHeaderProps, open, values }}
        />

        {open && (
          <FacetItemsList {...facetItemListProps}>
            <FacetItem
              active={true}
              name="Foo/Bar"
              onClick={jest.fn()}
              value="bar"
              stat={10}
              {...facetItemProps}
            />
            <FacetItem
              active={false}
              name="Foo/Baz"
              onClick={jest.fn()}
              value="baz"
              {...facetItemProps}
            />
          </FacetItemsList>
        )}
      </FacetBox>
    );
  }

  return renderComponent(<Facet />);
}
