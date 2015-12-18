import React from 'react';


const Truncated = ({ children, title }) => (
    <span
        className="code-truncated"
        data-title={title}
        data-toggle="tooltip">
      {children}
    </span>
);


export default Truncated;
