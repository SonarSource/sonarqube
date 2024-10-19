# Design System
This module is a Component Library for SonarQube. Despite being an internal module, it should be thought of as an external library.



## Components

Components implemented here should be generic components, mostly agnostic of the business domains.
There are some grey areas, like the Quality Gate Indicator which is obviously tied to the states a QG can have. 


## L10n, i18n, translations

Translation helpers (`translate`/`translateWithParameters`) cannot be used in the component library. Most text should be expected as a prop anyway.
Generic (read "context-independent") labels will soon be translated using react-intl.

Date/time formatting should use react-intl, to benefit from the user's locale.


## Helpers and utilities

Only helpers necessary for Components should be implemented in this module. Business logic utilities, or application-specific methods (e.g. `getComponentUrl`), should be kept in the core.