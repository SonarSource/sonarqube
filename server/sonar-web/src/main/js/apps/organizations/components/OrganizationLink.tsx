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
import * as React from 'react';
import Link from "../../../components/common/Link";

interface Props {
  children?: React.ReactNode;
  organization: { kee: string };

  [x: string]: any;
}

function handleClick(){
  setTimeout(()=>{
    window.location.reload();
  },1000);
  }

export default function OrganizationLink(props: Props) {
  const { children, organization, ...other } = props;

  return (
      <Link onClick={handleClick} to={`/organizations/${organization.kee}`} {...other}>
        {children}
      </Link>
  );
}
