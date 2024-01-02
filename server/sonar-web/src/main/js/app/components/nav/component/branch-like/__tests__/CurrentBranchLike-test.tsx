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
import { shallow } from 'enzyme';
import * as React from 'react';
import {
  mockProjectGithubBindingResponse,
  mockProjectGitLabBindingResponse,
} from '../../../../../../helpers/mocks/alm-settings';
import { mockMainBranch } from '../../../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../../../helpers/mocks/component';
import { ComponentQualifier } from '../../../../../../types/component';
import { CurrentBranchLike, CurrentBranchLikeProps } from '../CurrentBranchLike';

describe('applications', () => {
  it('should render correctly when there is only one branch and the user can admin the application', () => {
    const wrapper = shallowRender({
      component: mockComponent({
        configuration: { showSettings: true },
        qualifier: ComponentQualifier.Application,
      }),
      hasManyBranches: false,
    });
    expect(wrapper).toMatchSnapshot();
  });

  it("should render correctly when there is only one branch and the user CAN'T admin the application", () => {
    const wrapper = shallowRender({
      component: mockComponent({
        configuration: { showSettings: false },
        qualifier: ComponentQualifier.Application,
      }),
      hasManyBranches: false,
    });
    expect(wrapper).toMatchSnapshot();
  });

  it('should render correctly when there are many branchlikes', () => {
    const wrapper = shallowRender({
      branchesEnabled: true,
      component: mockComponent({
        qualifier: ComponentQualifier.Application,
      }),
      hasManyBranches: true,
    });
    expect(wrapper).toMatchSnapshot();
  });
});

describe('projects', () => {
  it('should render correctly when branches support is disabled', () => {
    expect(
      shallowRender({
        branchesEnabled: false,
        component: mockComponent({
          qualifier: ComponentQualifier.Project,
        }),
      })
    ).toMatchSnapshot('default');
    expect(
      shallowRender({
        branchesEnabled: false,
        component: mockComponent({
          qualifier: ComponentQualifier.Project,
        }),
        projectBinding: mockProjectGithubBindingResponse(),
      })
    ).toMatchSnapshot('alm with prs');
    expect(
      shallowRender({
        branchesEnabled: false,
        component: mockComponent({
          qualifier: ComponentQualifier.Project,
        }),
        projectBinding: mockProjectGitLabBindingResponse(),
      })
    ).toMatchSnapshot('alm with mrs');
  });

  it('should render correctly when there is only one branchlike', () => {
    const wrapper = shallowRender({
      branchesEnabled: true,
      component: mockComponent({
        qualifier: ComponentQualifier.Project,
      }),
      hasManyBranches: false,
    });
    expect(wrapper).toMatchSnapshot();
  });

  it('should render correctly when there are many branchlikes', () => {
    const wrapper = shallowRender({
      branchesEnabled: true,
      component: mockComponent({
        qualifier: ComponentQualifier.Project,
      }),
      hasManyBranches: true,
    });
    expect(wrapper).toMatchSnapshot();
  });
});

function shallowRender(props?: Partial<CurrentBranchLikeProps>) {
  return shallow(
    <CurrentBranchLike
      branchesEnabled={false}
      component={mockComponent()}
      currentBranchLike={mockMainBranch()}
      hasManyBranches={false}
      {...props}
    />
  );
}
