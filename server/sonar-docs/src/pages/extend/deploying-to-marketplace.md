---
title: Deploying to the Marketplace
url: /extend/deploying-to-marketplace/
---

If you have developed a SonarQube plugin, and it meets the requirements, we can even add it in the [SonarQube Marketplace](/instance-administration/marketplace/).

## Requirements
If your plugin meets the following requirements, then you can ask SonarSource (via the [Community Forum](https://community.sonarsource.com/c/plugins)) to reference your plugin in the [SonarQube Marketplace](/instance-administration/marketplace/):

1. Your plugin is open-source
   1. Source is freely accessible
   1. The license is a known FLOSS one (check [the list provided by the Open Source Initiative](http://opensource.org/licenses))
   1. There's a public issue tracking system
   1. Documentation is available online in English
   1. Binaries of each version are accessible somewhere
1. Releases follow open source conventions. For each release, the following must be available:
   1. release notes that reflect all significant changes in the version
   1. plugin jar
1. The key of your plugin must be:
   1. short and unique
   1. lowercase (no camelcase)
   1. composed only of [a-z0-9]
   1. related to the name of your plugin
   1. not just the name of a language (e.g. cannot be java, rust, js/javascript, ...)  
   examples of good keys: motionchart, communityphp, scmactivity
1. The description of your plugin must not be misleading in terms of content (the code needs to do pretty much what the name and description say it does). 
1. On initial entry into the Marketplace, SonarSource staff will test the plugin to verify reasonable functionality and quality. SonarSource staff must be provided with the necessary means to do this testing *without* the need to sign any agreements or fill out any forms. Ideally, the means to do this testing will be made available to the community at large, with the same lack of restrictions.
1. Your plugin does not compete with existing or soon-to-be-released SonarSource products (sorry, but we gotta pay the bills somehow).
1. It is analyzed on [SonarCloud](https://sonarcloud.io/) and the quality gate is green when doing a release.
1. It is compatible with the platform requirements (e.g. it runs on the minimum listed JRE).
1. If your plugin adds analysis of a language which is not analyzed by any SonarSource analyzer you must provide the NCLOC and NCLOC_DATA [metrics](/user-guide/metric-definitions/), which are both required to make the user experience within SonarQube consistent. You can take a look at how those metrics are provided by SonarJava ([NCLOC](https://github.com/SonarSource/sonar-java/blob/4cb1065f405edccbb7d229633945b3c56aeab04c/java-frontend/src/main/java/org/sonar/java/Measurer.java#L109), [NCLOC_DATA](https://github.com/SonarSource/sonar-java/blob/4cb1065f405edccbb7d229633945b3c56aeab04c/java-frontend/src/main/java/org/sonar/java/ast/visitors/FileLinesVisitor.java#L101)). 
1. Last but not least: your plugin must be aligned with the goal of the SonarQube platform: management of the technical debt and the quality of the code.  
To be more precise: every feature of SonarQube is tied to the code, so if your plugin provides data that can't be attached to a source or a test file, then there are chances that your plugin won't be accepted in the Marketplace

If your plugin meets these requirements, feel free to start a new thread on the Community Forum requesting inclusion. This thread should include:

* plugin description
* plugin home page url
* plugin project homepage on SonarCloud
* the link to a PR adding a file for your plugin to the sonar-update-center-properties repo, and the elements of a "new release" email listed below.

[[info]]
| ![](/images/info.svg) We reserve the right to exclude from the Marketplace plugins that we feel would be a dis-service to the community.

## Announcing new releases
When you've got a new release that should be published in the Marketplace, please:

* create a PR on the [sonar-update-center-properties repo](https://github.com/SonarSource/sonar-update-center-properties) updating the file for your plugin with the data for your new release
* start a new topic on the Community Forum with the following information:
   * Subject: [NEW RELEASE] Plugin Name & version
   * Body contains:
      * Short description: a few words about what's new in this version.
      * SonarQube compatibility: unchanged or specific versions.
      * Link to SonarCloud project dashboard so that we can check the quality gate status
      * Link to your PR
   * If it is the first release of the plugin, please mention that the plugin should be added to the Plugin Library page. (Otherwise, we're likely to forget!)

Once this thread is created, someone from SonarSource will review your PR and perform the manual steps to make the version available in the Marketplace.


## How to fill in the `sonar-update-center-properties` files

### Initial creation

#### Create file
In https://github.com/SonarSource/sonar-update-center-properties

File name should correspond to plugin's `pluginKey` and end with a `.properties` extension. Plugin key is set in the plugin module's pom (not the top-level pom):

* Explicitly in a `sonar.pluginKey` property. This is the first choice / preferred
* Implicitly by the artifactId:
   * `sonar-{pluginKey}-plugin`
   * when the `sonar-x-plugin` pattern is not used for the artifactId, the plugin key will be the whole artifact id.

#### Populate file
Provide the following meta values:

* `category` - one of: Coverage, Developer Tools, External Analyzers, Governance, Integration, Languages, Localization, Visualization/Reporting
* `description`
* `homepageUrl`
* `archivedVersions`=\[ leave this blank for now ]
* `publicVersions`=\[versionId] 
 
* `defaults.mavenGroupId`=\[the Maven `groupId`]
* `defaults.mavenArtifactId`=\[value of the top-level `artifactId`]

For the initially listed version create the following block:

* `[versionId].description`=\[free text. Spaces allowed. No quoting required]
* `[versionId].sqVersions`=\[compatibility information. See 'Filling in sqVersions compatibility ranges' below]
* `[versionId].date`=\[release date with format: YYYY-MM-DD]
* `[versionId].changelogUrl`=
* `[versionId].downloadUrl`=

The full list of meta information that can be provided (potentially overriding pom file values) can be found on [GitHub](https://github.com/SonarSource/sonar-update-center/blob/master/sonar-update-center-common/src/main/java/org/sonar/updatecenter/common/Plugin.java#L154).


#### Register file
Add file name (without `.properties` extension) to `plugins` value in https://github.com/SonarSource/sonar-update-center-properties/blob/master/update-center-source.properties


### Updating for new releases
Create a new block in the file with this format: 

* `[versionId].description`=\[free text. Spaces allowed. No quoting required]
* `[versionId].sqVersions`=\[compatibility information. See 'Filling in sqVersions compatibility ranges' below]
* `[versionId].date`=\[release date with format: YYYY-MM-DD]
* `[versionId].changelogUrl`=
* `[versionId].downloadUrl`=
Add `[versionId]` to the `publicVersions` list. Move to `archivedVersions` any versions with identical compatibility. See also 'Filling in sqVersions, publicVersions, and archivedVersions' below


### Filling in `sqVersions`, `publicVersions` and `archivedVersions`

The global field `publicVersions` is a comma-delimited list of plugin versions which should be offered to the user in the Marketplace and listed in the Plugin Version Matrix.

* Compatibility of Public versions cannot overlap
* Multiple versions can be in publicVersions if the versions of SonarQube they are compatible with do not overlap. 

The global field `archivedVersions` is a comma-delimited list of no-longer-preferred plugin versions. If a user has an archived version of a plugin installed, the Marketplace will offer an upgrade to the relevant public version. Upgrades will not be offered for plugin versions which are not found in `archivedVersions`.

* Compatibility of Archived versions can overlap
* If new version and previous version are compatible with the same versions of SonarQube, move the previous version into `archivedVersions`.

The `sqVersions` field of a release block gives the versions of SonarQube with which the plugin version is compatible. 

* Compatibility can be with a range, with a single version, or with a list of versions / ranges
* Compatibility is generally listed as a range in the form of [start,end]
* The value of start should be a SonarQube version number. 
* The value used for end may either be a version number or the special string `LATEST`.
* Only one version of a plugin can be compatible with `LATEST`, and it must be the most recent release
* Compatibility of public versions cannot overlap, so if necessary edit the range end for the older version to stop just before the newer version's compatibility starts. 
* You can use a wildcard at the end of a range, but not at the beginning.
   * ![](/images/check.svg) `[6.7,6.7.*]`
   * ![](/images/cross.svg) `[6.7.*,LATEST]`
* Multiple entries in a compatibility list should be comma-delimited, E.G. `5.5,[6.7,6.7.*],[7.3,LATEST]`

## Suggestions to manage your plugin development
A project hosted in a GitHub repository can easily meet the requirements:

* Sources are on Git - and you can easily configure them to be built by Travis CI
* GitHub Issues can be used as a bug tracking system
* GitHub Wiki or `README.md` can be used to write the documentation
* GitHub Releases can be used to publish your binaries

You can obviously use the [Community Forum](https://community.sonarsource.com/c/plugins) to ask for feedback on your plugin. You may want to post an RFF (Request for Feedback) before a release although it is not required. If you do, please close the thread before final release with a "feedback period closed" notice.


## Plugin deprecation

Occasionally, there's a need to deprecate a plugin. Typically for one or more of the following reasons:

* the functionality is obsolete or relies on deprecated platform functionality.
* It's no longer maintained by its authors and is buggy.
* It's no longer compatible with supported versions of the SonarQube platform.

In such case, the plugin is removed from the Marketplace.

## FAQ
**Q.** What should the release candidate announcement look like?  
This is up to you, but ideally, it will contain a:
* link to download the RC
* link to the version change log 
* deadline for feedback

Also, you should probably mention the contributors to the version if you didn't handle it solo.

**Q.** How long should the feedback period be?  
Again, that's up to you; it's your plugin. At SonarSource, when we put out a Release Candidate (we don't always & its optional for you too), we use a minimum 72 hour feedback period (with variations for holidays, weekends, and significant feedback).

**Q.**  Who can give feedback?  
Anyone! In fact, the more feedback the better. That's what makes developing in a community so wonderful. We just ask that when you have feedback, you keep it polite and respectful.

**Q.**  What if I don't get any feedback on my release candidate?  
You have two choices: agitate for more attention or consider no news to be good news and proceed with your release.

**Q.**  What if I get feedback that should block the release?  
The normal course of action here is to address the feedback and put out another release candidate. Typically, you would extend the feedback period to give people time to test the new version.

**Q.**  What happens when the feedback period is over?  
If you didn't get any feedback that you feel should block the release, then send a "period closed" notification on the same thread, perform the release, and in a separate thread ask that the new version be added to the Marketplace.

**Q.**  Should the initial release of a plugin be handled any differently than subsequent releases?  
Not necessarily, although it's probably more critical to get feedback on an initial release. So if there's no response within the initial feedback period, you should probably agitate for more attention, instead of assuming that no news is good news. It's up to you, though.

**Q.**  Who performs the release process?  
You do.

**Q.**  Where should the jars be posted for download?  
Up to you. If you're using GitHub to host your source code, then the easiest thing to do is create a project release and post downloads there.

**Q.**  What should the release notes look like?  
In the best case, it will be a publicly accessible list of work tickets handled in the version, similar to what you can get from Jira or GitHub Issues. At minimum, it will be an outline of the work done. In either case, it must reflect all significant changes.
