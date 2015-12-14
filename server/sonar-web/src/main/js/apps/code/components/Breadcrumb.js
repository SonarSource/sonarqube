import React from 'react';

import ComponentName from './ComponentName';


const Breadcrumb = ({ component, onBrowse }) => (
    <ComponentName
        component={component}
        onBrowse={onBrowse}/>
);


export default Breadcrumb;
