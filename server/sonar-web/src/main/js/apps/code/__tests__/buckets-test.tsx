/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { addComponent, addComponentChildren, getComponent, getComponentChildren } from '../bucket';

const component: T.ComponentMeasure = { key: 'frodo', name: 'frodo', qualifier: 'frodo' };

const componentKey: string = 'foo';
const childrenA: T.ComponentMeasure[] = [
  { key: 'foo', name: 'foo', qualifier: 'foo' },
  { key: 'bar', name: 'bar', qualifier: 'bar' }
];
const childrenB: T.ComponentMeasure[] = [
  { key: 'bart', name: 'bart', qualifier: 'bart' },
  { key: 'simpson', name: 'simpson', qualifier: 'simpson' }
];

it('should have empty bucket at start', () => {
  expect(getComponent(component.key)).toBeUndefined();
});

it('should be able to store components in a bucket', () => {
  addComponent(component);
  expect(getComponent(component.key)).toEqual(component);
});

it('should have empty children bucket at start', () => {
  expect(getComponentChildren(componentKey)).toBeUndefined();
});

it('should be able to store children components in a bucket', () => {
  addComponentChildren(componentKey, childrenA, childrenA.length, 1);
  expect(getComponentChildren(componentKey).children).toEqual(childrenA);
});

it('should append new children components at the end of the bucket', () => {
  addComponentChildren(componentKey, childrenB, 4, 2);
  const finalBucket = getComponentChildren(componentKey);
  expect(finalBucket.children).toEqual([...childrenA, ...childrenB]);
  expect(finalBucket.total).toBe(4);
  expect(finalBucket.page).toBe(2);
});
