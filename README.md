Sonar Core for for better integration tests
==========

This fork aims to generalize Coverage and ITCoverage. 
Up to now Coverage is modular and can be implemented by many plugins like cobertura, clover, emma, jacoco, etc. 
But IT Coverage can only be achived by Jacoco IT plugin. 

This fork aims to extend the coverage mechanism to IT coverage so that many modules can be used to run integration tests.

It is born as a need for android projects to separate unit tests from integration tests.
For instance we would like to get this kind of configuration tests : 
* Emma will be used to get coverage of integration tests in a standard android app-test project.
* Undercover will be used to get coverage of unit tests using Roboelectric or powermockito.

