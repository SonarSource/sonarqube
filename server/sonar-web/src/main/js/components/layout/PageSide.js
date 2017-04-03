/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import { css, media } from 'glamor';

type Props = {
  children?: React.Element<*>,
  top?: number
};

const width = css(
  {
    width: 'calc(50vw - 360px)'
  },
  media('(max-width: 1320px)', { width: 300 })
);

const sideStyles = css(width, {
  flexGrow: 0,
  flexShrink: 0,
  borderRight: '1px solid #e6e6e6',
  backgroundColor: '#f3f3f3'
});

const sideStickyStyles = css(width, {
  position: 'fixed',
  zIndex: 40,
  top: 0,
  bottom: 0,
  left: 0,
  overflowY: 'auto',
  overflowX: 'hidden',
  backgroundColor: '#f3f3f3'
});

const sideInnerStyles = css(
  {
    width: 300,
    marginLeft: 'calc(50vw - 660px)',
    backgroundColor: '#f3f3f3'
  },
  media('(max-width: 1320px)', { marginLeft: 0 })
);

const PageSide = (props: Props) => (
  <div className={sideStyles}>
    <div className={sideStickyStyles} style={{ top: props.top || 30 }}>
      <div className={sideInnerStyles}>
        {props.children}
      </div>
    </div>
  </div>
);

export default PageSide;
