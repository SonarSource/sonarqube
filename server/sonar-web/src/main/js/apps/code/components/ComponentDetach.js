import React from 'react';

import { getComponentUrl } from '../../../helpers/urls';


const ComponentDetach = ({ component }) => (
    <a
        className="icon-detach"
        target="_blank"
        href={getComponentUrl(component.key)}/>
);


export default ComponentDetach;
