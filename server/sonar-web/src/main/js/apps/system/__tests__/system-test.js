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
import { shallow, mount } from 'enzyme';
import ItemValue from '../item-value';

describe('System', function () {

  describe('Item Value', function () {
    it('should render string', () => {
      const result = shallow(<ItemValue value="/some/path/as/an/example"/>);
      expect(result.find('code').text()).toBe('/some/path/as/an/example');
    });

    it('should render `true`', () => {
      const result = mount(<ItemValue value={true}/>);
      expect(result.find('.icon-check').length).toBe(1);
    });

    it('should render `false`', () => {
      const result = mount(<ItemValue value={false}/>);
      expect(result.find('.icon-delete').length).toBe(1);
    });

    it('should render object', () => {
      const result = mount(<ItemValue value={{ name: 'Java', version: '3.2' }}/>);
      expect(result.find('table').length).toBe(1);
      expect(result.find('tr').length).toBe(2);
    });

    it('should render `true` inside object', () => {
      const result = mount(<ItemValue value={{ name: 'Java', isCool: true }}/>);
      expect(result.find('table').length).toBe(1);
      expect(result.find('.icon-check').length).toBe(1);
    });

    it('should render object inside object', () => {
      const result = mount(<ItemValue value={{ users: { docs: 1, shards: 5 }, tests: { docs: 68, shards: 5 } }}/>);
      expect(result.find('table').length).toBe(3);
      expect(result.find('tr').length).toBe(6);
    });
  });

  describe('Log Level', function () {
    it('should render select box', () => {
      const result = mount(<ItemValue value="INFO" name="Logs Level"/>);
      expect(result.find('select').length).toBe(1);
      expect(result.find('option').length).toBe(3);
    });

    it('should set initial value', () => {
      const result = mount(<ItemValue value="DEBUG" name="Logs Level"/>);
      expect(result.find('select').prop('value')).toBe('DEBUG');
    });

    it('should render warning', () => {
      const result = mount(<ItemValue value="DEBUG" name="Logs Level"/>);
      expect(result.find('.alert').length).toBe(1);
    });

    it('should not render warning', () => {
      const result = mount(<ItemValue value="INFO" name="Logs Level"/>);
      expect(result.find('.alert').length).toBe(0);
    });
  });

});
