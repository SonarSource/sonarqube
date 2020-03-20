/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import UpdateCenterMetaDataInjector from '../UpdateCenterMetaDataInjector';

it('should render correctly', () => {
  (global as any).document.body.innerHTML = `
<div class="page-container">
  <p>Lorem ipsum</p>
  <!-- update_center:java -->
  <p>Dolor sit amet</p>
  <!-- update_center : python -->
  <p>Foo Bar</p>
  <!--update_center       :       abap-->
</div>
`;

  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();

  (global as any).document.body.innerHTML = `
<div class="page-container">
  <p>Lorem ipsum</p>
  <!-- update_center:csharp -->
  <p>Foo Bar</p>
</div>
`;

  wrapper.setProps({ location: { pathname: 'foo2' } });
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<UpdateCenterMetaDataInjector['props']> = {}) {
  return shallow<UpdateCenterMetaDataInjector>(
    <UpdateCenterMetaDataInjector location={{ pathname: 'foo' }} {...props} />
  );
}
