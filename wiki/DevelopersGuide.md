![BrandHeader-STEM-BlackPrint.png](https://bitbucket.org/repo/b9GKML/images/1347427446-BrandHeader-STEM-BlackPrint.png)
# MASS-Developers Guide #
## Overview ##
The instructions in this article are intended primarily for those UWB CSS students actively engaged in MASS core library development. It is assumed that the developer has basic working knowledge of Java application development and Git usage. In particular, all developers should be able to perform the following operations using Git:

* Repository cloning
* Branching and Merging
* Conflict resolution

If you are unfamiliar with one or more of these operations, please consult the following resources:

* [The authoritative reference on Git](https://git-scm.com/book/en/v2)
* [A tutorial on using Atlassian's Sourcetree Git Client](https://confluence.atlassian.com/bitbucket/tutorial-learn-sourcetree-with-bitbucket-cloud-760120235.html)

## Working With the Repository ##
Please observe the following guidelines when working with the repository:
* Only commit files that you have created or modified. For example, sometimes an IDE will create workstation-specific configuration files that could cause other people using the same IDE problems when working on this project. If you only commit files that you have created or changed, you avoid adding unrelated files that can cause problems for others.
* Do not add JARs (or other binary artifacts) to the repository. A source-code revision control system is an inappropriate place to distribute compiled artifacts. Update the Maven POM to include any external resources that are required.
* Commit log messages are public! Please refrain from using obscene language, and provide useful comments that indicate what changes the commit represents.
* As you work, making sure to merge FROM develop INTO your branch as necessary to keep your branch updated as other developers conclude their work.
* Your work should always be performed on a branch from "develop" (new features, enhancements, or minor bugfixes) or "master" (fixing major bugs in a released version). You should not be committing changes directly to the "develop" or "master" branches.
* Name your branches in the following manner: '<your UW Netid>-<develop/hotfix>-<short feature description>'. For example, your branch might be named: 'jeckert-develop-mobile_ring_counters'.
* Commit and push often. As you commit, add the following text to your commit messages: '[#<issue number>]'. For example, as 'jeckert' worked on his mobile ring counters, he might commit a change with the following message: 'Added a simulator for the 6AV6 triode to improve performance [#7768]'

## Project Versioning With Maven ##
### Overview ###


## Workflow ##
There are different development activities depending on the nature of the work being performed. In general, there are four different workflows:

* Major new feature development or enhancements
* Resolving minor issues documented in the issue tracker
* Fixing critical bugs in the current released version
* Releasing a new version of MASS-Core

The following instructions may refer to the following links:
* [Issue Tracker](https://bitbucket.org/mass_library_developers/mass_java_core/issues)
* [Wiki](https://bitbucket.org/mass_library_developers/mass_java_core/wiki)

### General Notes ###
For all workflows, please note the following:
* If you are not already working from an issue, create a new issue in the issue tracker that describes in general the work to be performed. The "Kind" of issue will most likely either be "enhancement" or "task". Make sure to assign yourself as the "Assignee". This issue will serve as a place to put design documentation, notes, and other such information you feel necessary to document your enhancement. Note the issue number, it will be used later during the development process.
* Don't forget to merge/delete your branch and change the issue status to "Resolved" once your project is complete.

### Major New Feature/Enhancement Development ###
#### Overview ####
This workflow is used when major changes are to be made in MASS-Core. Typically a long-running branch will be used for the development activities.
#### Workflow Steps ####
1. Create a new issue or assign an existing issue to yourself
2. Create a branch from "develop"
3. Perform your work on this new branch
4. Merge/delete your branch to "develop" and resolve the issue

### Resolving Minor Issues ###
#### Overview ####
Use this workflow when making minor changes in response to an issue in the tracker.
#### Workflow Steps ####
1. Assign the existing issue to yourself
2. Create a branch from "develop"
3. Perform your work on this new branch
4. Merge/delete your branch to "develop" and resolve the issue

### Fixing Critical Bugs ###
#### Overview ####
Use this workflow when resolving major bugs in a released version of MASS-Core.
#### Workflow Steps ####
1. Create a new issue or assign an existing issue to yourself
2. Create a branch from "master". Your branch name should be like: 'tnicely-hotfix-fdiv-error'.
3. Perform your work on this new branch. Your work in this branch should be confined to fixing the problem ONLY.
4. Once work is complete, increment the minor version in the POM (don't forget to commit!).
5. Merge/delete your branch to "master", tag this commit with the new version, and resolve the issue.
6. Switch to the "develop" branch and merge your changes FROM "master" TO the "develop" branch.

### Releasing a New Version of MASS-Core ###
#### Overview ####
This workflow is used when it is decided that enhancements to MASS are considered ready to release.
#### Workflow Steps ####
1. In the "develop" branch, change the version type from "snapshot" to "release" and update the version number to reflect that nature of the changes since the last release. 
2. Commit the updated POM(s).
3. Switch to the "master" branch and merge FROM "develop" TO "master".
4. Tag this latest commit in the "master" branch with the complete version number.
5. Switch to the "develop" branch and merge your changes from "master" to the "develop" branch.
6. Increment the minor version in "develop" and switch the version type back to "snapshot".

