/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { shallow } from 'enzyme';
import * as React from 'react';
import { findDOMNode } from 'react-dom';
import ScreenPositionFixer from '../../controls/ScreenPositionFixer';
import { Popup, PopupArrow, PopupPlacement, PortalPopup } from '../popups';

jest.mock('react-dom', () => ({
  ...jest.requireActual('react-dom'),
  findDOMNode: jest.fn().mockReturnValue(undefined),
}));

describe('Popup', () => {
  it('should render Popup', () => {
    expect(
      shallow(
        <Popup
          arrowStyle={{ top: -5 }}
          className="foo"
          placement={PopupPlacement.LeftTop}
          style={{ left: -5 }}
        />
      )
    ).toMatchSnapshot();
  });

  it('should render PopupArrow', () => {
    expect(shallow(<PopupArrow style={{ left: -5 }} />)).toMatchSnapshot();
  });
});

describe('PortalPopup', () => {
  it('should render correctly without overlay', () => {
    expect(shallowRender({ overlay: undefined })).toMatchSnapshot();
  });

  it('should render correctly with overlay', () => {
    const wrapper = shallowRender();
    wrapper.setState({ left: 0, top: 0, width: 10, height: 10 });
    expect(wrapper).toMatchSnapshot();
    expect(wrapper.find(ScreenPositionFixer).dive().dive().dive()).toMatchSnapshot();
  });

  it('should correctly compute the popup positioning', () => {
    const fakeDomNode = document.createElement('div');
    fakeDomNode.getBoundingClientRect = jest
      .fn()
      .mockReturnValue({ left: 10, top: 10, width: 10, height: 10 });
    (findDOMNode as jest.Mock).mockReturnValue(fakeDomNode);
    const wrapper = shallowRender();
    const getPlacementSpy = jest.spyOn(wrapper.instance(), 'getPlacement');

    wrapper.instance().popupNode = {
      current: {
        getBoundingClientRect: jest.fn().mockReturnValue({ width: 8, height: 8 }),
      } as any,
    };

    wrapper.instance().positionPopup();
    expect(wrapper.state()).toEqual(expect.objectContaining({ left: 11, top: 20 }));

    getPlacementSpy.mockReturnValue(PopupPlacement.BottomLeft);
    wrapper.instance().positionPopup();
    expect(wrapper.state()).toEqual(expect.objectContaining({ left: 10, top: 20 }));

    getPlacementSpy.mockReturnValue(PopupPlacement.BottomRight);
    wrapper.instance().positionPopup();
    expect(wrapper.state()).toEqual(expect.objectContaining({ left: 12, top: 20 }));

    getPlacementSpy.mockReturnValue(PopupPlacement.LeftTop);
    wrapper.instance().positionPopup();
    expect(wrapper.state()).toEqual(expect.objectContaining({ left: 2, top: 10 }));

    getPlacementSpy.mockReturnValue(PopupPlacement.RightBottom);
    wrapper.instance().positionPopup();
    expect(wrapper.state()).toEqual(expect.objectContaining({ left: 20, top: 12 }));

    getPlacementSpy.mockReturnValue(PopupPlacement.RightTop);
    wrapper.instance().positionPopup();
    expect(wrapper.state()).toEqual(expect.objectContaining({ left: 20, top: 10 }));

    getPlacementSpy.mockReturnValue(PopupPlacement.TopLeft);
    wrapper.instance().positionPopup();
    expect(wrapper.state()).toEqual(expect.objectContaining({ left: 10, top: 2 }));
  });

  it('should correctly compute the popup arrow positioning', () => {
    const wrapper = shallowRender({ arrowOffset: -2 });
    const getPlacementSpy = jest.spyOn(wrapper.instance(), 'getPlacement');

    expect(
      wrapper.instance().adjustArrowPosition(PopupPlacement.BottomLeft, { leftFix: 10, topFix: 10 })
    ).toEqual({ marginLeft: -12 });

    expect(
      wrapper
        .instance()
        .adjustArrowPosition(PopupPlacement.RightBottom, { leftFix: 10, topFix: 10 })
    ).toEqual({ marginTop: -12 });
  });

  function shallowRender(props: Partial<PortalPopup['props']> = {}) {
    return shallow<PortalPopup>(
      <PortalPopup overlay={<span id="overlay" />} {...props}>
        <div id="popup-trigger" />
      </PortalPopup>
    );
  }
});
