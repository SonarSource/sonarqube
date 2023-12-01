/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { ComponentClass, FunctionComponent } from 'react';

export type ComponentPropsType<
  T extends ComponentClass | FunctionComponent<React.PropsWithChildren<any>>,
> = T extends ComponentClass<infer P>
  ? P
  : T extends FunctionComponent<React.PropsWithChildren<infer P>>
  ? P
  : never;

export function mockIntersectionObserver(): Function {
  let callback: Function;

  // @ts-ignore
  global.IntersectionObserver = jest.fn((cb: Function) => {
    const instance = {
      observe: jest.fn(),
      unobserve: jest.fn(),
      disconnect: jest.fn(),
    };

    callback = cb;

    callback([
      {
        isIntersecting: true,
        intersectionRatio: 1,
        boundingClientRect: { top: 0 },
        intersectionRect: { top: 0 },
      },
    ]);
    return instance;
  });

  return (entry: IntersectionObserverEntry) => callback([entry]);
}
