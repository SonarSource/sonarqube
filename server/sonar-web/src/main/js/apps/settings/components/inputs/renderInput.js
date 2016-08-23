/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import InputForString from './InputForString';
import InputForText from './InputForText';
import InputForPassword from './InputForPassword';
import InputForBoolean from './InputForBoolean';
import InputForInteger from './InputForInteger';
import InputForSingleSelectList from './InputForSingleSelectList';
import {
    TYPE_BOOLEAN,
    TYPE_INTEGER,
    TYPE_LONG,
    TYPE_TEXT,
    TYPE_PASSWORD,
    TYPE_FLOAT,
    TYPE_SINGLE_SELECT_LIST
} from '../../constants';

const renderInput = setting => {
  const { definition } = setting;

  if (definition.type === TYPE_TEXT) {
    return <InputForText setting={setting}/>;
  }

  if (definition.type === TYPE_PASSWORD) {
    return <InputForPassword setting={setting}/>;
  }

  if (definition.type === TYPE_BOOLEAN) {
    return <InputForBoolean setting={setting}/>;
  }

  if ([TYPE_INTEGER, TYPE_LONG, TYPE_FLOAT].includes(definition.type)) {
    return <InputForInteger setting={setting}/>;
  }

  if (definition.type === TYPE_SINGLE_SELECT_LIST) {
    return <InputForSingleSelectList setting={setting}/>;
  }

  return <InputForString setting={setting}/>;
};

export default renderInput;
