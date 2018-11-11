# RepoSync Maven Plugin
Used to prepare a remote Maven repository to be used exclusively (i.e.: without Central) for project builds.

## Goals Overview
* [reposync:update](./update-mojo.html) Update a remote repository

## Usage
### Installing a dependency
The execution below will deploy the following to repository `file://C:/myrepo`

* The dependency's artifact
* The dependency's transitive dependency artifacts
* The POMs of the artifact versions that were disqualified as transitive dependencies
 
`mvn com.kerbaya.maven:reposync-maven-plugin:update -DrepositoryUrl=file://C:/myrepo -Dartifact=junit:junit:4.12`

### Installing a dependency with JavaDocs and sources
The execution below will deploy the following to repository `file://C:/myrepo`

* The dependency's artifact
* The dependency's transitive dependency artifacts
* The JavaDoc and sources of the dependency and its transitive dependencies
* The POMs of the artifact versions that were disqualified as transitive dependencies
 
`mvn com.kerbaya.maven:reposync-maven-plugin:update -DrepositoryUrl=file://C:/myrepo -Dartifact=junit:junit:4.12 -Djar:javadoc,jar:sources`
