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
import { render } from '../../helpers/testUtils';
import { FCProps } from '../../types/misc';
import { Tooltip, TooltipInner } from '../Tooltip';

jest.mock('react-dom', () => {
  const reactDom = jest.requireActual('react-dom') as object;
  return { ...reactDom, findDOMNode: jest.fn().mockReturnValue(undefined) };
});

describe('TooltipInner', () => {
  it('should open & close', async () => {
    const onShow = jest.fn();
    const onHide = jest.fn();
    const { user } = setupWithProps({ onHide, onShow });

    await user.hover(screen.getByRole('note'));
    expect(await screen.findByRole('tooltip')).toBeInTheDocument();
    expect(onShow).toHaveBeenCalled();

    await user.unhover(screen.getByRole('note'));
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    expect(onHide).toHaveBeenCalled();
  });

  it('should not shadow children pointer events', async () => {
    const onShow = jest.fn();
    const onHide = jest.fn();
    const onPointerEnter = jest.fn();
    const onPointerLeave = jest.fn();
    const { user } = setupWithProps(
      { onHide, onShow },
      <div onPointerEnter={onPointerEnter} onPointerLeave={onPointerLeave} role="note" />,
    );

    await user.hover(screen.getByRole('note'));
    expect(await screen.findByRole('tooltip')).toBeInTheDocument();
    expect(onShow).toHaveBeenCalled();
    expect(onPointerEnter).toHaveBeenCalled();

    await user.unhover(screen.getByRole('note'));
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    expect(onHide).toHaveBeenCalled();
    expect(onPointerLeave).toHaveBeenCalled();
  });

  it('should not open when mouse goes away quickly', async () => {
    const { user } = setupWithProps();

    await user.hover(screen.getByRole('note'));
    await user.unhover(screen.getByRole('note'));

    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  });

  it('should position the tooltip correctly', async () => {
    const onShow = jest.fn();
    const onHide = jest.fn();
    const { user } = setupWithProps({ onHide, onShow });

    await user.hover(screen.getByRole('note'));
    expect(await screen.findByRole('tooltip')).toBeInTheDocument();
    expect(screen.getByRole('tooltip')).toHaveClass('bottom');
  });

  it('should be opened/hidden using tab navigation', async () => {
    const { user } = setupWithProps({}, <a href="#">Link</a>);

    await user.tab();
    expect(await screen.findByRole('tooltip')).toBeInTheDocument();
    await user.tab();
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  });

  function setupWithProps(
    props: Partial<TooltipInner['props']> = {},
    children = <div role="note" />,
  ) {
    return render(
      <TooltipInner content={<span id="overlay" />} mouseLeaveDelay={0} {...props}>
        {children}
      </TooltipInner>,
    );
  }
});

describe('Tooltip', () => {
  it('should not render tooltip without overlay', async () => {
    const { user } = setupWithProps({ content: undefined });
    await user.hover(screen.getByRole('note'));
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  });

  it('should not render undefined tooltips', async () => {
    const { user } = setupWithProps({ content: undefined, visible: true });
    await user.hover(screen.getByRole('note'));
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  });

  it('should not render empty tooltips', async () => {
    const { user } = setupWithProps({ content: '', visible: true });
    await user.hover(screen.getByRole('note'));
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  });

  function setupWithProps(
    props: Partial<FCProps<typeof Tooltip>> = {},
    children = <div role="note" />,
  ) {
    return render(
      <Tooltip content={<span id="overlay" />} {...props}>
        {children}
      </Tooltip>,
    );
  }
});
