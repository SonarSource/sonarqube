import React from 'react';

import { getComponentUrl } from '../../../helpers/urls';


const ComponentDetach = ({ component }) => (
    <a
        className="icon-detach"
        target="_blank"
        title={window.t('code.open_in_new_tab')}
        data-toggle="tooltip"
        href={getComponentUrl(component.key)}/>
);


export default ComponentDetach;
