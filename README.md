
# JSON Lenses - Bidirectional transformations for JSON

[![Build Status][github-actions-shield]][github-actions-link]
[![Maven][maven-shield]][maven-link]
[![Javadoc][javadoc-shield]][javadoc-link]

[github-actions-shield]: https://github.com/yokota/json-lenses/workflows/build/badge.svg?branch=master
[github-actions-link]: https://github.com/yokota/json-lenses/actions
[maven-shield]: https://img.shields.io/maven-central/v/io.yokota/json-lenses-core.svg
[maven-link]: https://search.maven.org/#search%7Cga%7C1%7Cjson-lenses-core
[javadoc-shield]: https://javadoc.io/badge/io.yokota/json-lenses-core.svg?color=blue
[javadoc-link]: https://javadoc.io/doc/io.yokota/json-lenses-core


JSON Lenses is a library that provides bidirectional transformations for JSON.  JSON Lenses is implemented 
atop the JSON Patch standard.  The advantage of JSON Lenses is that you specify the transformation in one direction, 
and the library automatically generates the transformation in the other direction.  This Java implementation is adapted 
from [Project Cambria](https://github.com/inkandswitch/cambria-project), a Typescript lens library.

The following JSON Lenses are supported:

- AddProperty - add a property
	- name - the property name
	- defaultValue - the default value for the property
- ConvertValue - convert the value of a property
	- name - the property name
	- mapping - a set of forward and reverse mappings for the value
- HeadProperty - replace an array with its first element
	- name - the property name 
- HoistProperty - move a property up one level
	- host - the source object
	- name - the property name
- In - apply a list of lens to a subtree
	- name - the property name for the subtree
	- lens - the list of lens
- Map - apply a list of lens to an array
   - lens - the list of lens
- PlungeProperty	- move a property down one level
	- host - the destination object
	- name - the property name
- RemoveProperty - remove a property
	- name - the property name
	- defaultValue - the default value for the property
- RenameProperty - rename a property
	- source - the source name
	- target - the target name
- WrapProperty - replace a value with an array containing the value
	- name - the property name

