import React from 'react';

import Breadcrumb from './Breadcrumb';


const Breadcrumbs = ({ breadcrumbs, onBrowse }) => (
    <ul className="code-breadcrumbs">
      {breadcrumbs.map((component, index) => (
          <li key={component.key}>
            <Breadcrumb
                component={component}
                onBrowse={index + 1 < breadcrumbs.length ? onBrowse : null}/>
          </li>
      ))}
    </ul>
);


export default Breadcrumbs;
