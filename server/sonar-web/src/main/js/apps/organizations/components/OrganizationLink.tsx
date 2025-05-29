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
import * as React from 'react';
import Link from '../../../components/common/Link';
import { useCurrentUser } from './../../../app/components/current-user/CurrentUserContext';


interface Props {
  children?: React.ReactNode;
  organization: { kee: string };
  onClick?: () => void;
  [x: string]: any;
}


 export default function OrganizationLink(props: Props) {
  const { children, organization, onClick, ...other } = props;
  const { currentUser,setIsNotStandardOrg } = useCurrentUser();
    let isStandardOrg = currentUser.standardOrgs?.includes(organization.kee);

    const handleClick = (e: React.MouseEvent<HTMLAnchorElement, MouseEvent>) => {
      if (!isStandardOrg && setIsNotStandardOrg) {
        setIsNotStandardOrg(true);
      } else if (isStandardOrg && setIsNotStandardOrg) {
        setIsNotStandardOrg(false);
      }
      if (onClick) onClick();
    };
    if (isStandardOrg) {
      return (
        <Link onClick={handleClick} to={`/organizations/${organization.kee}`} {...other}>
          {children}
        </Link>
      );
    }
  
    return (
      <Link onClick={handleClick} to={`/account`} {...other}>
        {children}
      </Link>
    );
  }
