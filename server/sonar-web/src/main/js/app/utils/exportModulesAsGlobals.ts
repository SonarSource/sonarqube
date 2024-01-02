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
import * as EmotionReact from '@emotion/react';
import EmotionStyled from '@emotion/styled';
import * as DateFns from 'date-fns';
import Lodash from 'lodash';
import React from 'react';
import * as ReactDom from 'react-dom';
import * as ReactIntl from 'react-intl';
import ReactModal from 'react-modal';
import * as ReactRouterDom from 'react-router-dom';

/*
 * Expose dependencies to extensions
 */
export default function exportModulesAsGlobals() {
  const w = window as unknown as any;
  w.EmotionReact = EmotionReact;
  w.EmotionStyled = EmotionStyled;
  w.DateFns = DateFns;
  w.Lodash = Lodash;
  w.React = React;
  w.ReactDOM = ReactDom;
  w.ReactIntl = ReactIntl;
  w.ReactModal = ReactModal;
  w.ReactRouterDom = ReactRouterDom;
}
