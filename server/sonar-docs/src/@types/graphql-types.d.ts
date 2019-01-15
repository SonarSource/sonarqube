/* tslint:disable */

export interface Query {
  allSitePage: SitePageConnection | null;
  allSitePlugin: SitePluginConnection | null;
  allDirectory: DirectoryConnection | null;
  allFile: FileConnection | null;
  allMarkdownRemark: MarkdownRemarkConnection | null;
  sitePage: SitePage | null;
  sitePlugin: SitePlugin | null;
  site: Site | null;
  directory: Directory | null;
  file: File | null;
  markdownRemark: MarkdownRemark | null;
}

export interface AllSitePageQueryArgs {
  skip: number | null;
  limit: number | null;
  sort: sitePageConnectionSort | null;
  filter: filterSitePage | null;
}

export interface AllSitePluginQueryArgs {
  skip: number | null;
  limit: number | null;
  sort: sitePluginConnectionSort | null;
  filter: filterSitePlugin | null;
}

export interface AllDirectoryQueryArgs {
  skip: number | null;
  limit: number | null;
  sort: directoryConnectionSort | null;
  filter: filterDirectory | null;
}

export interface AllFileQueryArgs {
  skip: number | null;
  limit: number | null;
  sort: fileConnectionSort | null;
  filter: filterFile | null;
}

export interface AllMarkdownRemarkQueryArgs {
  skip: number | null;
  limit: number | null;
  sort: markdownRemarkConnectionSort | null;
  filter: filterMarkdownRemark | null;
}

export interface SitePageQueryArgs {
  jsonName: sitePageJsonNameQueryString | null;
  internalComponentName: sitePageInternalComponentNameQueryString | null;
  path: sitePagePathQueryString_2 | null;
  component: sitePageComponentQueryString | null;
  componentChunkName: sitePageComponentChunkNameQueryString | null;
  context: sitePageContextInputObject | null;
  pluginCreator: sitePagePluginCreatorInputObject | null;
  pluginCreatorId: sitePagePluginCreatorIdQueryString_2 | null;
  componentPath: sitePageComponentPathQueryString | null;
  id: sitePageIdQueryString_2 | null;
  internal: sitePageInternalInputObject_2 | null;
}

export interface SitePluginQueryArgs {
  resolve: sitePluginResolveQueryString_2 | null;
  id: sitePluginIdQueryString_2 | null;
  name: sitePluginNameQueryString_2 | null;
  version: sitePluginVersionQueryString_2 | null;
  pluginOptions: sitePluginPluginOptionsInputObject_2 | null;
  nodeAPIs: sitePluginNodeApIsQueryList_2 | null;
  browserAPIs: sitePluginBrowserApIsQueryList_2 | null;
  ssrAPIs: sitePluginSsrApIsQueryList_2 | null;
  pluginFilepath: sitePluginPluginFilepathQueryString_2 | null;
  packageJson: sitePluginPackageJsonInputObject_2 | null;
  internal: sitePluginInternalInputObject_2 | null;
}

export interface SiteQueryArgs {
  siteMetadata: siteSiteMetadataInputObject_2 | null;
  port: sitePortQueryString_2 | null;
  host: siteHostQueryString_2 | null;
  pathPrefix: sitePathPrefixQueryString_2 | null;
  polyfill: sitePolyfillQueryBoolean_2 | null;
  buildTime: siteBuildTimeQueryString_2 | null;
  id: siteIdQueryString_2 | null;
  internal: siteInternalInputObject_2 | null;
}

export interface DirectoryQueryArgs {
  id: directoryIdQueryString_2 | null;
  internal: directoryInternalInputObject_2 | null;
  sourceInstanceName: directorySourceInstanceNameQueryString_2 | null;
  absolutePath: directoryAbsolutePathQueryString_2 | null;
  relativePath: directoryRelativePathQueryString_2 | null;
  extension: directoryExtensionQueryString_2 | null;
  size: directorySizeQueryInteger_2 | null;
  prettySize: directoryPrettySizeQueryString_2 | null;
  modifiedTime: directoryModifiedTimeQueryString_2 | null;
  accessTime: directoryAccessTimeQueryString_2 | null;
  changeTime: directoryChangeTimeQueryString_2 | null;
  birthTime: directoryBirthTimeQueryString_2 | null;
  root: directoryRootQueryString_2 | null;
  dir: directoryDirQueryString_2 | null;
  base: directoryBaseQueryString_2 | null;
  ext: directoryExtQueryString_2 | null;
  name: directoryNameQueryString_2 | null;
  relativeDirectory: directoryRelativeDirectoryQueryString_2 | null;
  dev: directoryDevQueryInteger_2 | null;
  mode: directoryModeQueryInteger_2 | null;
  nlink: directoryNlinkQueryInteger_2 | null;
  uid: directoryUidQueryInteger_2 | null;
  gid: directoryGidQueryInteger_2 | null;
  rdev: directoryRdevQueryInteger_2 | null;
  blksize: directoryBlksizeQueryInteger_2 | null;
  ino: directoryInoQueryInteger_2 | null;
  blocks: directoryBlocksQueryInteger_2 | null;
  atimeMs: directoryAtimeMsQueryFloat_2 | null;
  mtimeMs: directoryMtimeMsQueryFloat_2 | null;
  ctimeMs: directoryCtimeMsQueryFloat_2 | null;
  birthtimeMs: directoryBirthtimeMsQueryFloat_2 | null;
  atime: directoryAtimeQueryString_2 | null;
  mtime: directoryMtimeQueryString_2 | null;
  ctime: directoryCtimeQueryString_2 | null;
  birthtime: directoryBirthtimeQueryString_2 | null;
}

export interface FileQueryArgs {
  id: fileIdQueryString_2 | null;
  internal: fileInternalInputObject_2 | null;
  sourceInstanceName: fileSourceInstanceNameQueryString_2 | null;
  absolutePath: fileAbsolutePathQueryString_2 | null;
  relativePath: fileRelativePathQueryString_2 | null;
  extension: fileExtensionQueryString_2 | null;
  size: fileSizeQueryInteger_2 | null;
  prettySize: filePrettySizeQueryString_2 | null;
  modifiedTime: fileModifiedTimeQueryString_2 | null;
  accessTime: fileAccessTimeQueryString_2 | null;
  changeTime: fileChangeTimeQueryString_2 | null;
  birthTime: fileBirthTimeQueryString_2 | null;
  root: fileRootQueryString_2 | null;
  dir: fileDirQueryString_2 | null;
  base: fileBaseQueryString_2 | null;
  ext: fileExtQueryString_2 | null;
  name: fileNameQueryString_2 | null;
  relativeDirectory: fileRelativeDirectoryQueryString_2 | null;
  dev: fileDevQueryInteger_2 | null;
  mode: fileModeQueryInteger_2 | null;
  nlink: fileNlinkQueryInteger_2 | null;
  uid: fileUidQueryInteger_2 | null;
  gid: fileGidQueryInteger_2 | null;
  rdev: fileRdevQueryInteger_2 | null;
  blksize: fileBlksizeQueryInteger_2 | null;
  ino: fileInoQueryInteger_2 | null;
  blocks: fileBlocksQueryInteger_2 | null;
  atimeMs: fileAtimeMsQueryFloat_2 | null;
  mtimeMs: fileMtimeMsQueryFloat_2 | null;
  ctimeMs: fileCtimeMsQueryFloat_2 | null;
  birthtimeMs: fileBirthtimeMsQueryFloat_2 | null;
  atime: fileAtimeQueryString_2 | null;
  mtime: fileMtimeQueryString_2 | null;
  ctime: fileCtimeQueryString_2 | null;
  birthtime: fileBirthtimeQueryString_2 | null;
  publicURL: publicUrlQueryString_3 | null;
}

export interface MarkdownRemarkQueryArgs {
  id: markdownRemarkIdQueryString_2 | null;
  internal: markdownRemarkInternalInputObject_2 | null;
  frontmatter: markdownRemarkFrontmatterInputObject_2 | null;
  rawMarkdownBody: markdownRemarkRawMarkdownBodyQueryString_2 | null;
  fileAbsolutePath: markdownRemarkFileAbsolutePathQueryString_2 | null;
  fields: markdownRemarkFieldsInputObject_2 | null;
  html: htmlQueryString_3 | null;
  excerpt: excerptQueryString_3 | null;
  headings: headingsQueryList_3 | null;
  timeToRead: timeToReadQueryInt_3 | null;
  tableOfContents: tableOfContentsQueryString_3 | null;
  wordCount: wordCountTypeName_3 | null;
}

export interface sitePageConnectionSort {
  fields: Array<SitePageConnectionSortByFieldsEnum>;
  order: sitePageConnectionSortOrderValues | null;
}

export type SitePageConnectionSortByFieldsEnum =
  | 'jsonName'
  | 'internalComponentName'
  | 'path'
  | 'component'
  | 'componentChunkName'
  | 'context___slug'
  | 'pluginCreator___NODE'
  | 'pluginCreatorId'
  | 'componentPath'
  | 'id'
  | 'internal___type'
  | 'internal___contentDigest'
  | 'internal___description'
  | 'internal___owner';

export type sitePageConnectionSortOrderValues = 'ASC' | 'DESC';

export interface filterSitePage {
  jsonName: sitePageConnectionJsonNameQueryString | null;
  internalComponentName: sitePageConnectionInternalComponentNameQueryString | null;
  path: sitePageConnectionPathQueryString_2 | null;
  component: sitePageConnectionComponentQueryString | null;
  componentChunkName: sitePageConnectionComponentChunkNameQueryString | null;
  context: sitePageConnectionContextInputObject | null;
  pluginCreator: sitePageConnectionPluginCreatorInputObject | null;
  pluginCreatorId: sitePageConnectionPluginCreatorIdQueryString_2 | null;
  componentPath: sitePageConnectionComponentPathQueryString | null;
  id: sitePageConnectionIdQueryString_2 | null;
  internal: sitePageConnectionInternalInputObject_2 | null;
}

export interface sitePageConnectionJsonNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionInternalComponentNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPathQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionComponentQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionComponentChunkNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionContextInputObject {
  slug: sitePageConnectionContextSlugQueryString | null;
}

export interface sitePageConnectionContextSlugQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorInputObject {
  resolve: sitePageConnectionPluginCreatorResolveQueryString | null;
  id: sitePageConnectionPluginCreatorIdQueryString | null;
  name: sitePageConnectionPluginCreatorNameQueryString | null;
  version: sitePageConnectionPluginCreatorVersionQueryString | null;
  pluginOptions: sitePageConnectionPluginCreatorPluginOptionsInputObject | null;
  nodeAPIs: sitePageConnectionPluginCreatorNodeApIsQueryList | null;
  browserAPIs: sitePageConnectionPluginCreatorBrowserApIsQueryList | null;
  ssrAPIs: sitePageConnectionPluginCreatorSsrApIsQueryList | null;
  pluginFilepath: sitePageConnectionPluginCreatorPluginFilepathQueryString | null;
  packageJson: sitePageConnectionPluginCreatorPackageJsonInputObject | null;
  internal: sitePageConnectionPluginCreatorInternalInputObject | null;
}

export interface sitePageConnectionPluginCreatorResolveQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorIdQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorVersionQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsInputObject {
  plugins: sitePageConnectionPluginCreatorPluginOptionsPluginsQueryList | null;
  name: sitePageConnectionPluginCreatorPluginOptionsNameQueryString | null;
  path: sitePageConnectionPluginCreatorPluginOptionsPathQueryString | null;
  pathToConfigModule: sitePageConnectionPluginCreatorPluginOptionsPathToConfigModuleQueryString | null;
  blocks: sitePageConnectionPluginCreatorPluginOptionsBlocksInputObject | null;
  pathCheck: sitePageConnectionPluginCreatorPluginOptionsPathCheckQueryBoolean | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsPluginsQueryList {
  elemMatch: sitePageConnectionPluginCreatorPluginOptionsPluginsInputObject | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsPluginsInputObject {
  resolve: sitePageConnectionPluginCreatorPluginOptionsPluginsResolveQueryString | null;
  id: sitePageConnectionPluginCreatorPluginOptionsPluginsIdQueryString | null;
  name: sitePageConnectionPluginCreatorPluginOptionsPluginsNameQueryString | null;
  version: sitePageConnectionPluginCreatorPluginOptionsPluginsVersionQueryString | null;
  pluginOptions: sitePageConnectionPluginCreatorPluginOptionsPluginsPluginOptionsInputObject | null;
  pluginFilepath: sitePageConnectionPluginCreatorPluginOptionsPluginsPluginFilepathQueryString | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsPluginsResolveQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsPluginsIdQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsPluginsNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsPluginsVersionQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsPluginsPluginOptionsInputObject {
  blocks: sitePageConnectionPluginCreatorPluginOptionsPluginsPluginOptionsBlocksInputObject | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsPluginsPluginOptionsBlocksInputObject {
  danger: sitePageConnectionPluginCreatorPluginOptionsPluginsPluginOptionsBlocksDangerQueryString | null;
  warning: sitePageConnectionPluginCreatorPluginOptionsPluginsPluginOptionsBlocksWarningQueryString | null;
  info: sitePageConnectionPluginCreatorPluginOptionsPluginsPluginOptionsBlocksInfoQueryString | null;
  success: sitePageConnectionPluginCreatorPluginOptionsPluginsPluginOptionsBlocksSuccessQueryString | null;
  collapse: sitePageConnectionPluginCreatorPluginOptionsPluginsPluginOptionsBlocksCollapseQueryString | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsPluginsPluginOptionsBlocksDangerQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsPluginsPluginOptionsBlocksWarningQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsPluginsPluginOptionsBlocksInfoQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsPluginsPluginOptionsBlocksSuccessQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsPluginsPluginOptionsBlocksCollapseQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsPluginsPluginFilepathQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsPathQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsPathToConfigModuleQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsBlocksInputObject {
  danger: sitePageConnectionPluginCreatorPluginOptionsBlocksDangerQueryString | null;
  warning: sitePageConnectionPluginCreatorPluginOptionsBlocksWarningQueryString | null;
  info: sitePageConnectionPluginCreatorPluginOptionsBlocksInfoQueryString | null;
  success: sitePageConnectionPluginCreatorPluginOptionsBlocksSuccessQueryString | null;
  collapse: sitePageConnectionPluginCreatorPluginOptionsBlocksCollapseQueryString | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsBlocksDangerQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsBlocksWarningQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsBlocksInfoQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsBlocksSuccessQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsBlocksCollapseQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginOptionsPathCheckQueryBoolean {
  eq: boolean | null;
  ne: boolean | null;
  in: Array<boolean> | null;
  nin: Array<boolean> | null;
}

export interface sitePageConnectionPluginCreatorNodeApIsQueryList {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorBrowserApIsQueryList {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorSsrApIsQueryList {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPluginFilepathQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonInputObject {
  name: sitePageConnectionPluginCreatorPackageJsonNameQueryString | null;
  description: sitePageConnectionPluginCreatorPackageJsonDescriptionQueryString | null;
  version: sitePageConnectionPluginCreatorPackageJsonVersionQueryString | null;
  main: sitePageConnectionPluginCreatorPackageJsonMainQueryString | null;
  author: sitePageConnectionPluginCreatorPackageJsonAuthorQueryString | null;
  license: sitePageConnectionPluginCreatorPackageJsonLicenseQueryString | null;
  dependencies: sitePageConnectionPluginCreatorPackageJsonDependenciesQueryList | null;
  devDependencies: sitePageConnectionPluginCreatorPackageJsonDevDependenciesQueryList | null;
  peerDependencies: sitePageConnectionPluginCreatorPackageJsonPeerDependenciesQueryList | null;
  keywords: sitePageConnectionPluginCreatorPackageJsonKeywordsQueryList | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonDescriptionQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonVersionQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonMainQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonAuthorQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonLicenseQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonDependenciesQueryList {
  elemMatch: sitePageConnectionPluginCreatorPackageJsonDependenciesInputObject | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonDependenciesInputObject {
  name: sitePageConnectionPluginCreatorPackageJsonDependenciesNameQueryString | null;
  version: sitePageConnectionPluginCreatorPackageJsonDependenciesVersionQueryString | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonDependenciesNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonDependenciesVersionQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonDevDependenciesQueryList {
  elemMatch: sitePageConnectionPluginCreatorPackageJsonDevDependenciesInputObject | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonDevDependenciesInputObject {
  name: sitePageConnectionPluginCreatorPackageJsonDevDependenciesNameQueryString | null;
  version: sitePageConnectionPluginCreatorPackageJsonDevDependenciesVersionQueryString | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonDevDependenciesNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonDevDependenciesVersionQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonPeerDependenciesQueryList {
  elemMatch: sitePageConnectionPluginCreatorPackageJsonPeerDependenciesInputObject | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonPeerDependenciesInputObject {
  name: sitePageConnectionPluginCreatorPackageJsonPeerDependenciesNameQueryString | null;
  version: sitePageConnectionPluginCreatorPackageJsonPeerDependenciesVersionQueryString | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonPeerDependenciesNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonPeerDependenciesVersionQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorPackageJsonKeywordsQueryList {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorInternalInputObject {
  contentDigest: sitePageConnectionPluginCreatorInternalContentDigestQueryString | null;
  type: sitePageConnectionPluginCreatorInternalTypeQueryString | null;
  owner: sitePageConnectionPluginCreatorInternalOwnerQueryString | null;
}

export interface sitePageConnectionPluginCreatorInternalContentDigestQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorInternalTypeQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorInternalOwnerQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionPluginCreatorIdQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionComponentPathQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionIdQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionInternalInputObject_2 {
  type: sitePageConnectionInternalTypeQueryString_2 | null;
  contentDigest: sitePageConnectionInternalContentDigestQueryString_2 | null;
  description: sitePageConnectionInternalDescriptionQueryString | null;
  owner: sitePageConnectionInternalOwnerQueryString_2 | null;
}

export interface sitePageConnectionInternalTypeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionInternalContentDigestQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionInternalDescriptionQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageConnectionInternalOwnerQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface SitePageConnection {
  pageInfo: PageInfo;
  edges: Array<SitePageEdge> | null;
  totalCount: number | null;
  distinct: Array<string> | null;
  group: Array<sitePageGroupConnectionConnection> | null;
}

export interface DistinctSitePageConnectionArgs {
  field: sitePageDistinctEnum | null;
}

export interface GroupSitePageConnectionArgs {
  skip: number | null;
  limit: number | null;
  field: sitePageGroupEnum | null;
}

export interface PageInfo {
  hasNextPage: boolean;
}

export interface SitePageEdge {
  node: SitePage | null;
  next: SitePage | null;
  previous: SitePage | null;
}

export interface SitePage extends Node {
  id: string;
  parent: Node | null;
  children: Array<Node> | null;
  jsonName: string | null;
  internalComponentName: string | null;
  path: string | null;
  component: string | null;
  componentChunkName: string | null;
  context: context | null;
  pluginCreator: SitePlugin | null;
  pluginCreatorId: string | null;
  componentPath: string | null;
  internal: internal_7 | null;
}

export interface Node {
  id: string;
  parent: Node | null;
  children: Array<Node> | null;
}

export interface context {
  slug: string | null;
}

export interface SitePlugin extends Node {
  id: string;
  parent: Node | null;
  children: Array<Node> | null;
  resolve: string | null;
  name: string | null;
  version: string | null;
  pluginOptions: pluginOptions_3 | null;
  nodeAPIs: Array<string> | null;
  browserAPIs: Array<string> | null;
  ssrAPIs: Array<string> | null;
  pluginFilepath: string | null;
  packageJson: packageJson_2 | null;
  internal: internal_8 | null;
}

export interface pluginOptions_3 {
  plugins: Array<plugins_2> | null;
  name: string | null;
  path: string | null;
  pathToConfigModule: string | null;
  blocks: blocks_4 | null;
  pathCheck: boolean | null;
}

export interface plugins_2 {
  resolve: string | null;
  id: string | null;
  name: string | null;
  version: string | null;
  pluginOptions: pluginOptions_4 | null;
  pluginFilepath: string | null;
}

export interface pluginOptions_4 {
  blocks: blocks_3 | null;
}

export interface blocks_3 {
  danger: string | null;
  warning: string | null;
  info: string | null;
  success: string | null;
  collapse: string | null;
}

export interface blocks_4 {
  danger: string | null;
  warning: string | null;
  info: string | null;
  success: string | null;
  collapse: string | null;
}

export interface packageJson_2 {
  name: string | null;
  description: string | null;
  version: string | null;
  main: string | null;
  author: string | null;
  license: string | null;
  dependencies: Array<dependencies_2> | null;
  devDependencies: Array<devDependencies_2> | null;
  peerDependencies: Array<peerDependencies_2> | null;
  keywords: Array<string> | null;
}

export interface dependencies_2 {
  name: string | null;
  version: string | null;
}

export interface devDependencies_2 {
  name: string | null;
  version: string | null;
}

export interface peerDependencies_2 {
  name: string | null;
  version: string | null;
}

export interface internal_8 {
  contentDigest: string | null;
  type: string | null;
  owner: string | null;
}

export interface internal_7 {
  type: string | null;
  contentDigest: string | null;
  description: string | null;
  owner: string | null;
}

export type sitePageDistinctEnum =
  | 'jsonName'
  | 'internalComponentName'
  | 'path'
  | 'component'
  | 'componentChunkName'
  | 'context___slug'
  | 'pluginCreator___NODE'
  | 'pluginCreatorId'
  | 'componentPath'
  | 'id'
  | 'internal___type'
  | 'internal___contentDigest'
  | 'internal___description'
  | 'internal___owner';

export type sitePageGroupEnum =
  | 'jsonName'
  | 'internalComponentName'
  | 'path'
  | 'component'
  | 'componentChunkName'
  | 'context___slug'
  | 'pluginCreator___NODE'
  | 'pluginCreatorId'
  | 'componentPath'
  | 'id'
  | 'internal___type'
  | 'internal___contentDigest'
  | 'internal___description'
  | 'internal___owner';

export interface sitePageGroupConnectionConnection {
  pageInfo: PageInfo;
  edges: Array<sitePageGroupConnectionEdge> | null;
  field: string | null;
  fieldValue: string | null;
  totalCount: number | null;
}

export interface sitePageGroupConnectionEdge {
  node: SitePage | null;
  next: SitePage | null;
  previous: SitePage | null;
}

export interface sitePluginConnectionSort {
  fields: Array<SitePluginConnectionSortByFieldsEnum>;
  order: sitePluginConnectionSortOrderValues | null;
}

export type SitePluginConnectionSortByFieldsEnum =
  | 'resolve'
  | 'id'
  | 'name'
  | 'version'
  | 'pluginOptions___plugins'
  | 'pluginOptions___name'
  | 'pluginOptions___path'
  | 'pluginOptions___pathToConfigModule'
  | 'pluginOptions___blocks___danger'
  | 'pluginOptions___blocks___warning'
  | 'pluginOptions___blocks___info'
  | 'pluginOptions___blocks___success'
  | 'pluginOptions___blocks___collapse'
  | 'pluginOptions___pathCheck'
  | 'nodeAPIs'
  | 'browserAPIs'
  | 'ssrAPIs'
  | 'pluginFilepath'
  | 'packageJson___name'
  | 'packageJson___description'
  | 'packageJson___version'
  | 'packageJson___main'
  | 'packageJson___author'
  | 'packageJson___license'
  | 'packageJson___dependencies'
  | 'packageJson___devDependencies'
  | 'packageJson___peerDependencies'
  | 'packageJson___keywords'
  | 'internal___contentDigest'
  | 'internal___type'
  | 'internal___owner';

export type sitePluginConnectionSortOrderValues = 'ASC' | 'DESC';

export interface filterSitePlugin {
  resolve: sitePluginConnectionResolveQueryString_2 | null;
  id: sitePluginConnectionIdQueryString_2 | null;
  name: sitePluginConnectionNameQueryString_2 | null;
  version: sitePluginConnectionVersionQueryString_2 | null;
  pluginOptions: sitePluginConnectionPluginOptionsInputObject_2 | null;
  nodeAPIs: sitePluginConnectionNodeApIsQueryList_2 | null;
  browserAPIs: sitePluginConnectionBrowserApIsQueryList_2 | null;
  ssrAPIs: sitePluginConnectionSsrApIsQueryList_2 | null;
  pluginFilepath: sitePluginConnectionPluginFilepathQueryString_2 | null;
  packageJson: sitePluginConnectionPackageJsonInputObject_2 | null;
  internal: sitePluginConnectionInternalInputObject_2 | null;
}

export interface sitePluginConnectionResolveQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionIdQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionVersionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsInputObject_2 {
  plugins: sitePluginConnectionPluginOptionsPluginsQueryList_2 | null;
  name: sitePluginConnectionPluginOptionsNameQueryString_2 | null;
  path: sitePluginConnectionPluginOptionsPathQueryString_2 | null;
  pathToConfigModule: sitePluginConnectionPluginOptionsPathToConfigModuleQueryString_2 | null;
  blocks: sitePluginConnectionPluginOptionsBlocksInputObject_2 | null;
  pathCheck: sitePluginConnectionPluginOptionsPathCheckQueryBoolean_2 | null;
}

export interface sitePluginConnectionPluginOptionsPluginsQueryList_2 {
  elemMatch: sitePluginConnectionPluginOptionsPluginsInputObject_2 | null;
}

export interface sitePluginConnectionPluginOptionsPluginsInputObject_2 {
  resolve: sitePluginConnectionPluginOptionsPluginsResolveQueryString_2 | null;
  id: sitePluginConnectionPluginOptionsPluginsIdQueryString_2 | null;
  name: sitePluginConnectionPluginOptionsPluginsNameQueryString_2 | null;
  version: sitePluginConnectionPluginOptionsPluginsVersionQueryString_2 | null;
  pluginOptions: sitePluginConnectionPluginOptionsPluginsPluginOptionsInputObject_2 | null;
  pluginFilepath: sitePluginConnectionPluginOptionsPluginsPluginFilepathQueryString_2 | null;
}

export interface sitePluginConnectionPluginOptionsPluginsResolveQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsPluginsIdQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsPluginsNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsPluginsVersionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsPluginsPluginOptionsInputObject_2 {
  blocks: sitePluginConnectionPluginOptionsPluginsPluginOptionsBlocksInputObject_2 | null;
}

export interface sitePluginConnectionPluginOptionsPluginsPluginOptionsBlocksInputObject_2 {
  danger: sitePluginConnectionPluginOptionsPluginsPluginOptionsBlocksDangerQueryString_2 | null;
  warning: sitePluginConnectionPluginOptionsPluginsPluginOptionsBlocksWarningQueryString_2 | null;
  info: sitePluginConnectionPluginOptionsPluginsPluginOptionsBlocksInfoQueryString_2 | null;
  success: sitePluginConnectionPluginOptionsPluginsPluginOptionsBlocksSuccessQueryString_2 | null;
  collapse: sitePluginConnectionPluginOptionsPluginsPluginOptionsBlocksCollapseQueryString_2 | null;
}

export interface sitePluginConnectionPluginOptionsPluginsPluginOptionsBlocksDangerQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsPluginsPluginOptionsBlocksWarningQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsPluginsPluginOptionsBlocksInfoQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsPluginsPluginOptionsBlocksSuccessQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsPluginsPluginOptionsBlocksCollapseQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsPluginsPluginFilepathQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsPathQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsPathToConfigModuleQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsBlocksInputObject_2 {
  danger: sitePluginConnectionPluginOptionsBlocksDangerQueryString_2 | null;
  warning: sitePluginConnectionPluginOptionsBlocksWarningQueryString_2 | null;
  info: sitePluginConnectionPluginOptionsBlocksInfoQueryString_2 | null;
  success: sitePluginConnectionPluginOptionsBlocksSuccessQueryString_2 | null;
  collapse: sitePluginConnectionPluginOptionsBlocksCollapseQueryString_2 | null;
}

export interface sitePluginConnectionPluginOptionsBlocksDangerQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsBlocksWarningQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsBlocksInfoQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsBlocksSuccessQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsBlocksCollapseQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginOptionsPathCheckQueryBoolean_2 {
  eq: boolean | null;
  ne: boolean | null;
  in: Array<boolean> | null;
  nin: Array<boolean> | null;
}

export interface sitePluginConnectionNodeApIsQueryList_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionBrowserApIsQueryList_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionSsrApIsQueryList_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPluginFilepathQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPackageJsonInputObject_2 {
  name: sitePluginConnectionPackageJsonNameQueryString_2 | null;
  description: sitePluginConnectionPackageJsonDescriptionQueryString_2 | null;
  version: sitePluginConnectionPackageJsonVersionQueryString_2 | null;
  main: sitePluginConnectionPackageJsonMainQueryString_2 | null;
  author: sitePluginConnectionPackageJsonAuthorQueryString_2 | null;
  license: sitePluginConnectionPackageJsonLicenseQueryString_2 | null;
  dependencies: sitePluginConnectionPackageJsonDependenciesQueryList_2 | null;
  devDependencies: sitePluginConnectionPackageJsonDevDependenciesQueryList_2 | null;
  peerDependencies: sitePluginConnectionPackageJsonPeerDependenciesQueryList_2 | null;
  keywords: sitePluginConnectionPackageJsonKeywordsQueryList_2 | null;
}

export interface sitePluginConnectionPackageJsonNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPackageJsonDescriptionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPackageJsonVersionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPackageJsonMainQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPackageJsonAuthorQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPackageJsonLicenseQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPackageJsonDependenciesQueryList_2 {
  elemMatch: sitePluginConnectionPackageJsonDependenciesInputObject_2 | null;
}

export interface sitePluginConnectionPackageJsonDependenciesInputObject_2 {
  name: sitePluginConnectionPackageJsonDependenciesNameQueryString_2 | null;
  version: sitePluginConnectionPackageJsonDependenciesVersionQueryString_2 | null;
}

export interface sitePluginConnectionPackageJsonDependenciesNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPackageJsonDependenciesVersionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPackageJsonDevDependenciesQueryList_2 {
  elemMatch: sitePluginConnectionPackageJsonDevDependenciesInputObject_2 | null;
}

export interface sitePluginConnectionPackageJsonDevDependenciesInputObject_2 {
  name: sitePluginConnectionPackageJsonDevDependenciesNameQueryString_2 | null;
  version: sitePluginConnectionPackageJsonDevDependenciesVersionQueryString_2 | null;
}

export interface sitePluginConnectionPackageJsonDevDependenciesNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPackageJsonDevDependenciesVersionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPackageJsonPeerDependenciesQueryList_2 {
  elemMatch: sitePluginConnectionPackageJsonPeerDependenciesInputObject_2 | null;
}

export interface sitePluginConnectionPackageJsonPeerDependenciesInputObject_2 {
  name: sitePluginConnectionPackageJsonPeerDependenciesNameQueryString_2 | null;
  version: sitePluginConnectionPackageJsonPeerDependenciesVersionQueryString_2 | null;
}

export interface sitePluginConnectionPackageJsonPeerDependenciesNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPackageJsonPeerDependenciesVersionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionPackageJsonKeywordsQueryList_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionInternalInputObject_2 {
  contentDigest: sitePluginConnectionInternalContentDigestQueryString_2 | null;
  type: sitePluginConnectionInternalTypeQueryString_2 | null;
  owner: sitePluginConnectionInternalOwnerQueryString_2 | null;
}

export interface sitePluginConnectionInternalContentDigestQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionInternalTypeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginConnectionInternalOwnerQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface SitePluginConnection {
  pageInfo: PageInfo;
  edges: Array<SitePluginEdge> | null;
  totalCount: number | null;
  distinct: Array<string> | null;
  group: Array<sitePluginGroupConnectionConnection> | null;
}

export interface DistinctSitePluginConnectionArgs {
  field: sitePluginDistinctEnum | null;
}

export interface GroupSitePluginConnectionArgs {
  skip: number | null;
  limit: number | null;
  field: sitePluginGroupEnum | null;
}

export interface SitePluginEdge {
  node: SitePlugin | null;
  next: SitePlugin | null;
  previous: SitePlugin | null;
}

export type sitePluginDistinctEnum =
  | 'resolve'
  | 'id'
  | 'name'
  | 'version'
  | 'pluginOptions___plugins'
  | 'pluginOptions___name'
  | 'pluginOptions___path'
  | 'pluginOptions___pathToConfigModule'
  | 'pluginOptions___blocks___danger'
  | 'pluginOptions___blocks___warning'
  | 'pluginOptions___blocks___info'
  | 'pluginOptions___blocks___success'
  | 'pluginOptions___blocks___collapse'
  | 'pluginOptions___pathCheck'
  | 'nodeAPIs'
  | 'browserAPIs'
  | 'ssrAPIs'
  | 'pluginFilepath'
  | 'packageJson___name'
  | 'packageJson___description'
  | 'packageJson___version'
  | 'packageJson___main'
  | 'packageJson___author'
  | 'packageJson___license'
  | 'packageJson___dependencies'
  | 'packageJson___devDependencies'
  | 'packageJson___peerDependencies'
  | 'packageJson___keywords'
  | 'internal___contentDigest'
  | 'internal___type'
  | 'internal___owner';

export type sitePluginGroupEnum =
  | 'resolve'
  | 'id'
  | 'name'
  | 'version'
  | 'pluginOptions___plugins'
  | 'pluginOptions___name'
  | 'pluginOptions___path'
  | 'pluginOptions___pathToConfigModule'
  | 'pluginOptions___blocks___danger'
  | 'pluginOptions___blocks___warning'
  | 'pluginOptions___blocks___info'
  | 'pluginOptions___blocks___success'
  | 'pluginOptions___blocks___collapse'
  | 'pluginOptions___pathCheck'
  | 'nodeAPIs'
  | 'browserAPIs'
  | 'ssrAPIs'
  | 'pluginFilepath'
  | 'packageJson___name'
  | 'packageJson___description'
  | 'packageJson___version'
  | 'packageJson___main'
  | 'packageJson___author'
  | 'packageJson___license'
  | 'packageJson___dependencies'
  | 'packageJson___devDependencies'
  | 'packageJson___peerDependencies'
  | 'packageJson___keywords'
  | 'internal___contentDigest'
  | 'internal___type'
  | 'internal___owner';

export interface sitePluginGroupConnectionConnection {
  pageInfo: PageInfo;
  edges: Array<sitePluginGroupConnectionEdge> | null;
  field: string | null;
  fieldValue: string | null;
  totalCount: number | null;
}

export interface sitePluginGroupConnectionEdge {
  node: SitePlugin | null;
  next: SitePlugin | null;
  previous: SitePlugin | null;
}

export interface directoryConnectionSort {
  fields: Array<DirectoryConnectionSortByFieldsEnum>;
  order: directoryConnectionSortOrderValues | null;
}

export type DirectoryConnectionSortByFieldsEnum =
  | 'id'
  | 'internal___contentDigest'
  | 'internal___type'
  | 'internal___description'
  | 'internal___owner'
  | 'sourceInstanceName'
  | 'absolutePath'
  | 'relativePath'
  | 'extension'
  | 'size'
  | 'prettySize'
  | 'modifiedTime'
  | 'accessTime'
  | 'changeTime'
  | 'birthTime'
  | 'root'
  | 'dir'
  | 'base'
  | 'ext'
  | 'name'
  | 'relativeDirectory'
  | 'dev'
  | 'mode'
  | 'nlink'
  | 'uid'
  | 'gid'
  | 'rdev'
  | 'blksize'
  | 'ino'
  | 'blocks'
  | 'atimeMs'
  | 'mtimeMs'
  | 'ctimeMs'
  | 'birthtimeMs'
  | 'atime'
  | 'mtime'
  | 'ctime'
  | 'birthtime';

export type directoryConnectionSortOrderValues = 'ASC' | 'DESC';

export interface filterDirectory {
  id: directoryConnectionIdQueryString_2 | null;
  internal: directoryConnectionInternalInputObject_2 | null;
  sourceInstanceName: directoryConnectionSourceInstanceNameQueryString_2 | null;
  absolutePath: directoryConnectionAbsolutePathQueryString_2 | null;
  relativePath: directoryConnectionRelativePathQueryString_2 | null;
  extension: directoryConnectionExtensionQueryString_2 | null;
  size: directoryConnectionSizeQueryInteger_2 | null;
  prettySize: directoryConnectionPrettySizeQueryString_2 | null;
  modifiedTime: directoryConnectionModifiedTimeQueryString_2 | null;
  accessTime: directoryConnectionAccessTimeQueryString_2 | null;
  changeTime: directoryConnectionChangeTimeQueryString_2 | null;
  birthTime: directoryConnectionBirthTimeQueryString_2 | null;
  root: directoryConnectionRootQueryString_2 | null;
  dir: directoryConnectionDirQueryString_2 | null;
  base: directoryConnectionBaseQueryString_2 | null;
  ext: directoryConnectionExtQueryString_2 | null;
  name: directoryConnectionNameQueryString_2 | null;
  relativeDirectory: directoryConnectionRelativeDirectoryQueryString_2 | null;
  dev: directoryConnectionDevQueryInteger_2 | null;
  mode: directoryConnectionModeQueryInteger_2 | null;
  nlink: directoryConnectionNlinkQueryInteger_2 | null;
  uid: directoryConnectionUidQueryInteger_2 | null;
  gid: directoryConnectionGidQueryInteger_2 | null;
  rdev: directoryConnectionRdevQueryInteger_2 | null;
  blksize: directoryConnectionBlksizeQueryInteger_2 | null;
  ino: directoryConnectionInoQueryInteger_2 | null;
  blocks: directoryConnectionBlocksQueryInteger_2 | null;
  atimeMs: directoryConnectionAtimeMsQueryFloat_2 | null;
  mtimeMs: directoryConnectionMtimeMsQueryFloat_2 | null;
  ctimeMs: directoryConnectionCtimeMsQueryFloat_2 | null;
  birthtimeMs: directoryConnectionBirthtimeMsQueryFloat_2 | null;
  atime: directoryConnectionAtimeQueryString_2 | null;
  mtime: directoryConnectionMtimeQueryString_2 | null;
  ctime: directoryConnectionCtimeQueryString_2 | null;
  birthtime: directoryConnectionBirthtimeQueryString_2 | null;
}

export interface directoryConnectionIdQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionInternalInputObject_2 {
  contentDigest: directoryConnectionInternalContentDigestQueryString_2 | null;
  type: directoryConnectionInternalTypeQueryString_2 | null;
  description: directoryConnectionInternalDescriptionQueryString_2 | null;
  owner: directoryConnectionInternalOwnerQueryString_2 | null;
}

export interface directoryConnectionInternalContentDigestQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionInternalTypeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionInternalDescriptionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionInternalOwnerQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionSourceInstanceNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionAbsolutePathQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionRelativePathQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionExtensionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionSizeQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryConnectionPrettySizeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionModifiedTimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionAccessTimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionChangeTimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionBirthTimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionRootQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionDirQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionBaseQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionExtQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionRelativeDirectoryQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionDevQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryConnectionModeQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryConnectionNlinkQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryConnectionUidQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryConnectionGidQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryConnectionRdevQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryConnectionBlksizeQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryConnectionInoQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryConnectionBlocksQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryConnectionAtimeMsQueryFloat_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryConnectionMtimeMsQueryFloat_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryConnectionCtimeMsQueryFloat_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryConnectionBirthtimeMsQueryFloat_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryConnectionAtimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionMtimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionCtimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryConnectionBirthtimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface DirectoryConnection {
  pageInfo: PageInfo;
  edges: Array<DirectoryEdge> | null;
  totalCount: number | null;
  distinct: Array<string> | null;
  group: Array<directoryGroupConnectionConnection> | null;
}

export interface DistinctDirectoryConnectionArgs {
  field: directoryDistinctEnum | null;
}

export interface GroupDirectoryConnectionArgs {
  skip: number | null;
  limit: number | null;
  field: directoryGroupEnum | null;
}

export interface DirectoryEdge {
  node: Directory | null;
  next: Directory | null;
  previous: Directory | null;
}

export interface Directory extends Node {
  id: string;
  parent: Node | null;
  children: Array<Node> | null;
  internal: internal_9 | null;
  sourceInstanceName: string | null;
  absolutePath: string | null;
  relativePath: string | null;
  extension: string | null;
  size: number | null;
  prettySize: string | null;
  modifiedTime: Date | null;
  accessTime: Date | null;
  changeTime: Date | null;
  birthTime: Date | null;
  root: string | null;
  dir: string | null;
  base: string | null;
  ext: string | null;
  name: string | null;
  relativeDirectory: string | null;
  dev: number | null;
  mode: number | null;
  nlink: number | null;
  uid: number | null;
  gid: number | null;
  rdev: number | null;
  blksize: number | null;
  ino: number | null;
  blocks: number | null;
  atimeMs: number | null;
  mtimeMs: number | null;
  ctimeMs: number | null;
  birthtimeMs: number | null;
  atime: Date | null;
  mtime: Date | null;
  ctime: Date | null;
  birthtime: Date | null;
}

export interface ModifiedTimeDirectoryArgs {
  formatString: string | null;
  fromNow: boolean | null;
  difference: string | null;
  locale: string | null;
}

export interface AccessTimeDirectoryArgs {
  formatString: string | null;
  fromNow: boolean | null;
  difference: string | null;
  locale: string | null;
}

export interface ChangeTimeDirectoryArgs {
  formatString: string | null;
  fromNow: boolean | null;
  difference: string | null;
  locale: string | null;
}

export interface BirthTimeDirectoryArgs {
  formatString: string | null;
  fromNow: boolean | null;
  difference: string | null;
  locale: string | null;
}

export interface AtimeDirectoryArgs {
  formatString: string | null;
  fromNow: boolean | null;
  difference: string | null;
  locale: string | null;
}

export interface MtimeDirectoryArgs {
  formatString: string | null;
  fromNow: boolean | null;
  difference: string | null;
  locale: string | null;
}

export interface CtimeDirectoryArgs {
  formatString: string | null;
  fromNow: boolean | null;
  difference: string | null;
  locale: string | null;
}

export interface BirthtimeDirectoryArgs {
  formatString: string | null;
  fromNow: boolean | null;
  difference: string | null;
  locale: string | null;
}

export interface internal_9 {
  contentDigest: string | null;
  type: string | null;
  description: string | null;
  owner: string | null;
}

export type Date = any;

export type directoryDistinctEnum =
  | 'id'
  | 'internal___contentDigest'
  | 'internal___type'
  | 'internal___description'
  | 'internal___owner'
  | 'sourceInstanceName'
  | 'absolutePath'
  | 'relativePath'
  | 'extension'
  | 'size'
  | 'prettySize'
  | 'modifiedTime'
  | 'accessTime'
  | 'changeTime'
  | 'birthTime'
  | 'root'
  | 'dir'
  | 'base'
  | 'ext'
  | 'name'
  | 'relativeDirectory'
  | 'dev'
  | 'mode'
  | 'nlink'
  | 'uid'
  | 'gid'
  | 'rdev'
  | 'blksize'
  | 'ino'
  | 'blocks'
  | 'atimeMs'
  | 'mtimeMs'
  | 'ctimeMs'
  | 'birthtimeMs'
  | 'atime'
  | 'mtime'
  | 'ctime'
  | 'birthtime';

export type directoryGroupEnum =
  | 'id'
  | 'internal___contentDigest'
  | 'internal___type'
  | 'internal___description'
  | 'internal___owner'
  | 'sourceInstanceName'
  | 'absolutePath'
  | 'relativePath'
  | 'extension'
  | 'size'
  | 'prettySize'
  | 'modifiedTime'
  | 'accessTime'
  | 'changeTime'
  | 'birthTime'
  | 'root'
  | 'dir'
  | 'base'
  | 'ext'
  | 'name'
  | 'relativeDirectory'
  | 'dev'
  | 'mode'
  | 'nlink'
  | 'uid'
  | 'gid'
  | 'rdev'
  | 'blksize'
  | 'ino'
  | 'blocks'
  | 'atimeMs'
  | 'mtimeMs'
  | 'ctimeMs'
  | 'birthtimeMs'
  | 'atime'
  | 'mtime'
  | 'ctime'
  | 'birthtime';

export interface directoryGroupConnectionConnection {
  pageInfo: PageInfo;
  edges: Array<directoryGroupConnectionEdge> | null;
  field: string | null;
  fieldValue: string | null;
  totalCount: number | null;
}

export interface directoryGroupConnectionEdge {
  node: Directory | null;
  next: Directory | null;
  previous: Directory | null;
}

export interface fileConnectionSort {
  fields: Array<FileConnectionSortByFieldsEnum>;
  order: fileConnectionSortOrderValues | null;
}

export type FileConnectionSortByFieldsEnum =
  | 'id'
  | 'children'
  | 'internal___contentDigest'
  | 'internal___type'
  | 'internal___mediaType'
  | 'internal___description'
  | 'internal___owner'
  | 'sourceInstanceName'
  | 'absolutePath'
  | 'relativePath'
  | 'extension'
  | 'size'
  | 'prettySize'
  | 'modifiedTime'
  | 'accessTime'
  | 'changeTime'
  | 'birthTime'
  | 'root'
  | 'dir'
  | 'base'
  | 'ext'
  | 'name'
  | 'relativeDirectory'
  | 'dev'
  | 'mode'
  | 'nlink'
  | 'uid'
  | 'gid'
  | 'rdev'
  | 'blksize'
  | 'ino'
  | 'blocks'
  | 'atimeMs'
  | 'mtimeMs'
  | 'ctimeMs'
  | 'birthtimeMs'
  | 'atime'
  | 'mtime'
  | 'ctime'
  | 'birthtime'
  | 'publicURL';

export type fileConnectionSortOrderValues = 'ASC' | 'DESC';

export interface filterFile {
  id: fileConnectionIdQueryString_2 | null;
  internal: fileConnectionInternalInputObject_2 | null;
  sourceInstanceName: fileConnectionSourceInstanceNameQueryString_2 | null;
  absolutePath: fileConnectionAbsolutePathQueryString_2 | null;
  relativePath: fileConnectionRelativePathQueryString_2 | null;
  extension: fileConnectionExtensionQueryString_2 | null;
  size: fileConnectionSizeQueryInteger_2 | null;
  prettySize: fileConnectionPrettySizeQueryString_2 | null;
  modifiedTime: fileConnectionModifiedTimeQueryString_2 | null;
  accessTime: fileConnectionAccessTimeQueryString_2 | null;
  changeTime: fileConnectionChangeTimeQueryString_2 | null;
  birthTime: fileConnectionBirthTimeQueryString_2 | null;
  root: fileConnectionRootQueryString_2 | null;
  dir: fileConnectionDirQueryString_2 | null;
  base: fileConnectionBaseQueryString_2 | null;
  ext: fileConnectionExtQueryString_2 | null;
  name: fileConnectionNameQueryString_2 | null;
  relativeDirectory: fileConnectionRelativeDirectoryQueryString_2 | null;
  dev: fileConnectionDevQueryInteger_2 | null;
  mode: fileConnectionModeQueryInteger_2 | null;
  nlink: fileConnectionNlinkQueryInteger_2 | null;
  uid: fileConnectionUidQueryInteger_2 | null;
  gid: fileConnectionGidQueryInteger_2 | null;
  rdev: fileConnectionRdevQueryInteger_2 | null;
  blksize: fileConnectionBlksizeQueryInteger_2 | null;
  ino: fileConnectionInoQueryInteger_2 | null;
  blocks: fileConnectionBlocksQueryInteger_2 | null;
  atimeMs: fileConnectionAtimeMsQueryFloat_2 | null;
  mtimeMs: fileConnectionMtimeMsQueryFloat_2 | null;
  ctimeMs: fileConnectionCtimeMsQueryFloat_2 | null;
  birthtimeMs: fileConnectionBirthtimeMsQueryFloat_2 | null;
  atime: fileConnectionAtimeQueryString_2 | null;
  mtime: fileConnectionMtimeQueryString_2 | null;
  ctime: fileConnectionCtimeQueryString_2 | null;
  birthtime: fileConnectionBirthtimeQueryString_2 | null;
  publicURL: publicUrlQueryString_4 | null;
}

export interface fileConnectionIdQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionInternalInputObject_2 {
  contentDigest: fileConnectionInternalContentDigestQueryString_2 | null;
  type: fileConnectionInternalTypeQueryString_2 | null;
  mediaType: fileConnectionInternalMediaTypeQueryString_2 | null;
  description: fileConnectionInternalDescriptionQueryString_2 | null;
  owner: fileConnectionInternalOwnerQueryString_2 | null;
}

export interface fileConnectionInternalContentDigestQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionInternalTypeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionInternalMediaTypeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionInternalDescriptionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionInternalOwnerQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionSourceInstanceNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionAbsolutePathQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionRelativePathQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionExtensionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionSizeQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileConnectionPrettySizeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionModifiedTimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionAccessTimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionChangeTimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionBirthTimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionRootQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionDirQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionBaseQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionExtQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionRelativeDirectoryQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionDevQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileConnectionModeQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileConnectionNlinkQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileConnectionUidQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileConnectionGidQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileConnectionRdevQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileConnectionBlksizeQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileConnectionInoQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileConnectionBlocksQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileConnectionAtimeMsQueryFloat_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileConnectionMtimeMsQueryFloat_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileConnectionCtimeMsQueryFloat_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileConnectionBirthtimeMsQueryFloat_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileConnectionAtimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionMtimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionCtimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileConnectionBirthtimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface publicUrlQueryString_4 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface FileConnection {
  pageInfo: PageInfo;
  edges: Array<FileEdge> | null;
  totalCount: number | null;
  distinct: Array<string> | null;
  group: Array<fileGroupConnectionConnection> | null;
}

export interface DistinctFileConnectionArgs {
  field: fileDistinctEnum | null;
}

export interface GroupFileConnectionArgs {
  skip: number | null;
  limit: number | null;
  field: fileGroupEnum | null;
}

export interface FileEdge {
  node: File | null;
  next: File | null;
  previous: File | null;
}

export interface File extends Node {
  id: string;
  parent: Node | null;
  children: Array<Node> | null;
  childMarkdownRemark: MarkdownRemark | null;
  internal: internal_10 | null;
  sourceInstanceName: string | null;
  absolutePath: string | null;
  relativePath: string | null;
  extension: string | null;
  size: number | null;
  prettySize: string | null;
  modifiedTime: Date | null;
  accessTime: Date | null;
  changeTime: Date | null;
  birthTime: Date | null;
  root: string | null;
  dir: string | null;
  base: string | null;
  ext: string | null;
  name: string | null;
  relativeDirectory: string | null;
  dev: number | null;
  mode: number | null;
  nlink: number | null;
  uid: number | null;
  gid: number | null;
  rdev: number | null;
  blksize: number | null;
  ino: number | null;
  blocks: number | null;
  atimeMs: number | null;
  mtimeMs: number | null;
  ctimeMs: number | null;
  birthtimeMs: number | null;
  atime: Date | null;
  mtime: Date | null;
  ctime: Date | null;
  birthtime: Date | null;
  publicURL: string | null;
}

export interface ModifiedTimeFileArgs {
  formatString: string | null;
  fromNow: boolean | null;
  difference: string | null;
  locale: string | null;
}

export interface AccessTimeFileArgs {
  formatString: string | null;
  fromNow: boolean | null;
  difference: string | null;
  locale: string | null;
}

export interface ChangeTimeFileArgs {
  formatString: string | null;
  fromNow: boolean | null;
  difference: string | null;
  locale: string | null;
}

export interface BirthTimeFileArgs {
  formatString: string | null;
  fromNow: boolean | null;
  difference: string | null;
  locale: string | null;
}

export interface AtimeFileArgs {
  formatString: string | null;
  fromNow: boolean | null;
  difference: string | null;
  locale: string | null;
}

export interface MtimeFileArgs {
  formatString: string | null;
  fromNow: boolean | null;
  difference: string | null;
  locale: string | null;
}

export interface CtimeFileArgs {
  formatString: string | null;
  fromNow: boolean | null;
  difference: string | null;
  locale: string | null;
}

export interface BirthtimeFileArgs {
  formatString: string | null;
  fromNow: boolean | null;
  difference: string | null;
  locale: string | null;
}

export interface MarkdownRemark extends Node {
  id: string;
  parent: Node | null;
  children: Array<Node> | null;
  internal: internal_11 | null;
  frontmatter: frontmatter_2 | null;
  rawMarkdownBody: string | null;
  fileAbsolutePath: string | null;
  fields: fields_2 | null;
  html: string | null;
  htmlAst: JSON | null;
  excerpt: string | null;
  headings: Array<MarkdownHeading> | null;
  timeToRead: number | null;
  tableOfContents: string | null;
  wordCount: wordCount | null;
}

export interface ExcerptMarkdownRemarkArgs {
  pruneLength: number | null;
  truncate: boolean | null;
  format: ExcerptFormats | null;
}

export interface HeadingsMarkdownRemarkArgs {
  depth: HeadingLevels | null;
}

export interface TableOfContentsMarkdownRemarkArgs {
  pathToSlugField: string | null;
}

export interface internal_11 {
  content: string | null;
  type: string | null;
  contentDigest: string | null;
  owner: string | null;
  fieldOwners: fieldOwners_2 | null;
}

export interface fieldOwners_2 {
  slug: string | null;
}

export interface frontmatter_2 {
  title: string | null;
  nav: string | null;
  url: string | null;
}

export interface fields_2 {
  slug: string | null;
}

export type JSON = any;

export type ExcerptFormats = 'PLAIN' | 'HTML';

export type HeadingLevels = 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';

export interface MarkdownHeading {
  value: string | null;
  depth: number | null;
}

export interface wordCount {
  paragraphs: number | null;
  sentences: number | null;
  words: number | null;
}

export interface internal_10 {
  contentDigest: string | null;
  type: string | null;
  mediaType: string | null;
  description: string | null;
  owner: string | null;
}

export type fileDistinctEnum =
  | 'id'
  | 'children'
  | 'internal___contentDigest'
  | 'internal___type'
  | 'internal___mediaType'
  | 'internal___description'
  | 'internal___owner'
  | 'sourceInstanceName'
  | 'absolutePath'
  | 'relativePath'
  | 'extension'
  | 'size'
  | 'prettySize'
  | 'modifiedTime'
  | 'accessTime'
  | 'changeTime'
  | 'birthTime'
  | 'root'
  | 'dir'
  | 'base'
  | 'ext'
  | 'name'
  | 'relativeDirectory'
  | 'dev'
  | 'mode'
  | 'nlink'
  | 'uid'
  | 'gid'
  | 'rdev'
  | 'blksize'
  | 'ino'
  | 'blocks'
  | 'atimeMs'
  | 'mtimeMs'
  | 'ctimeMs'
  | 'birthtimeMs'
  | 'atime'
  | 'mtime'
  | 'ctime'
  | 'birthtime';

export type fileGroupEnum =
  | 'id'
  | 'children'
  | 'internal___contentDigest'
  | 'internal___type'
  | 'internal___mediaType'
  | 'internal___description'
  | 'internal___owner'
  | 'sourceInstanceName'
  | 'absolutePath'
  | 'relativePath'
  | 'extension'
  | 'size'
  | 'prettySize'
  | 'modifiedTime'
  | 'accessTime'
  | 'changeTime'
  | 'birthTime'
  | 'root'
  | 'dir'
  | 'base'
  | 'ext'
  | 'name'
  | 'relativeDirectory'
  | 'dev'
  | 'mode'
  | 'nlink'
  | 'uid'
  | 'gid'
  | 'rdev'
  | 'blksize'
  | 'ino'
  | 'blocks'
  | 'atimeMs'
  | 'mtimeMs'
  | 'ctimeMs'
  | 'birthtimeMs'
  | 'atime'
  | 'mtime'
  | 'ctime'
  | 'birthtime';

export interface fileGroupConnectionConnection {
  pageInfo: PageInfo;
  edges: Array<fileGroupConnectionEdge> | null;
  field: string | null;
  fieldValue: string | null;
  totalCount: number | null;
}

export interface fileGroupConnectionEdge {
  node: File | null;
  next: File | null;
  previous: File | null;
}

export interface markdownRemarkConnectionSort {
  fields: Array<MarkdownRemarkConnectionSortByFieldsEnum>;
  order: markdownRemarkConnectionSortOrderValues | null;
}

export type MarkdownRemarkConnectionSortByFieldsEnum =
  | 'id'
  | 'parent'
  | 'internal___content'
  | 'internal___type'
  | 'internal___contentDigest'
  | 'internal___owner'
  | 'internal___fieldOwners___slug'
  | 'frontmatter___title'
  | 'frontmatter___nav'
  | 'frontmatter___url'
  | 'rawMarkdownBody'
  | 'fileAbsolutePath'
  | 'fields___slug'
  | 'html'
  | 'excerpt'
  | 'headings'
  | 'timeToRead'
  | 'tableOfContents'
  | 'wordCount___paragraphs'
  | 'wordCount___sentences'
  | 'wordCount___words';

export type markdownRemarkConnectionSortOrderValues = 'ASC' | 'DESC';

export interface filterMarkdownRemark {
  id: markdownRemarkConnectionIdQueryString_2 | null;
  internal: markdownRemarkConnectionInternalInputObject_2 | null;
  frontmatter: markdownRemarkConnectionFrontmatterInputObject_2 | null;
  rawMarkdownBody: markdownRemarkConnectionRawMarkdownBodyQueryString_2 | null;
  fileAbsolutePath: markdownRemarkConnectionFileAbsolutePathQueryString_2 | null;
  fields: markdownRemarkConnectionFieldsInputObject_2 | null;
  html: htmlQueryString_4 | null;
  excerpt: excerptQueryString_4 | null;
  headings: headingsQueryList_4 | null;
  timeToRead: timeToReadQueryInt_4 | null;
  tableOfContents: tableOfContentsQueryString_4 | null;
  wordCount: wordCountTypeName_4 | null;
}

export interface markdownRemarkConnectionIdQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkConnectionInternalInputObject_2 {
  content: markdownRemarkConnectionInternalContentQueryString_2 | null;
  type: markdownRemarkConnectionInternalTypeQueryString_2 | null;
  contentDigest: markdownRemarkConnectionInternalContentDigestQueryString_2 | null;
  owner: markdownRemarkConnectionInternalOwnerQueryString_2 | null;
  fieldOwners: markdownRemarkConnectionInternalFieldOwnersInputObject_2 | null;
}

export interface markdownRemarkConnectionInternalContentQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkConnectionInternalTypeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkConnectionInternalContentDigestQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkConnectionInternalOwnerQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkConnectionInternalFieldOwnersInputObject_2 {
  slug: markdownRemarkConnectionInternalFieldOwnersSlugQueryString_2 | null;
}

export interface markdownRemarkConnectionInternalFieldOwnersSlugQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkConnectionFrontmatterInputObject_2 {
  title: markdownRemarkConnectionFrontmatterTitleQueryString_2 | null;
  nav: markdownRemarkConnectionFrontmatterNavQueryString_2 | null;
  url: markdownRemarkConnectionFrontmatterUrlQueryString_2 | null;
}

export interface markdownRemarkConnectionFrontmatterTitleQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkConnectionFrontmatterNavQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkConnectionFrontmatterUrlQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkConnectionRawMarkdownBodyQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkConnectionFileAbsolutePathQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkConnectionFieldsInputObject_2 {
  slug: markdownRemarkConnectionFieldsSlugQueryString_2 | null;
}

export interface markdownRemarkConnectionFieldsSlugQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface htmlQueryString_4 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface excerptQueryString_4 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface headingsQueryList_4 {
  elemMatch: headingsListElemTypeName_4 | null;
}

export interface headingsListElemTypeName_4 {
  value: headingsListElemValueQueryString_4 | null;
  depth: headingsListElemDepthQueryInt_4 | null;
}

export interface headingsListElemValueQueryString_4 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface headingsListElemDepthQueryInt_4 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface timeToReadQueryInt_4 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface tableOfContentsQueryString_4 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface wordCountTypeName_4 {
  paragraphs: wordCountParagraphsQueryInt_4 | null;
  sentences: wordCountSentencesQueryInt_4 | null;
  words: wordCountWordsQueryInt_4 | null;
}

export interface wordCountParagraphsQueryInt_4 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface wordCountSentencesQueryInt_4 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface wordCountWordsQueryInt_4 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface MarkdownRemarkConnection {
  pageInfo: PageInfo;
  edges: Array<MarkdownRemarkEdge> | null;
  totalCount: number | null;
  distinct: Array<string> | null;
  group: Array<markdownRemarkGroupConnectionConnection> | null;
}

export interface DistinctMarkdownRemarkConnectionArgs {
  field: markdownRemarkDistinctEnum | null;
}

export interface GroupMarkdownRemarkConnectionArgs {
  skip: number | null;
  limit: number | null;
  field: markdownRemarkGroupEnum | null;
}

export interface MarkdownRemarkEdge {
  node: MarkdownRemark | null;
  next: MarkdownRemark | null;
  previous: MarkdownRemark | null;
}

export type markdownRemarkDistinctEnum =
  | 'id'
  | 'parent'
  | 'internal___content'
  | 'internal___type'
  | 'internal___contentDigest'
  | 'internal___owner'
  | 'internal___fieldOwners___slug'
  | 'frontmatter___title'
  | 'frontmatter___nav'
  | 'frontmatter___url'
  | 'rawMarkdownBody'
  | 'fileAbsolutePath'
  | 'fields___slug';

export type markdownRemarkGroupEnum =
  | 'id'
  | 'parent'
  | 'internal___content'
  | 'internal___type'
  | 'internal___contentDigest'
  | 'internal___owner'
  | 'internal___fieldOwners___slug'
  | 'frontmatter___title'
  | 'frontmatter___nav'
  | 'frontmatter___url'
  | 'rawMarkdownBody'
  | 'fileAbsolutePath'
  | 'fields___slug';

export interface markdownRemarkGroupConnectionConnection {
  pageInfo: PageInfo;
  edges: Array<markdownRemarkGroupConnectionEdge> | null;
  field: string | null;
  fieldValue: string | null;
  totalCount: number | null;
}

export interface markdownRemarkGroupConnectionEdge {
  node: MarkdownRemark | null;
  next: MarkdownRemark | null;
  previous: MarkdownRemark | null;
}

export interface sitePageJsonNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageInternalComponentNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePathQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageComponentQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageComponentChunkNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageContextInputObject {
  slug: sitePageContextSlugQueryString | null;
}

export interface sitePageContextSlugQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorInputObject {
  resolve: sitePagePluginCreatorResolveQueryString | null;
  id: sitePagePluginCreatorIdQueryString | null;
  name: sitePagePluginCreatorNameQueryString | null;
  version: sitePagePluginCreatorVersionQueryString | null;
  pluginOptions: sitePagePluginCreatorPluginOptionsInputObject | null;
  nodeAPIs: sitePagePluginCreatorNodeApIsQueryList | null;
  browserAPIs: sitePagePluginCreatorBrowserApIsQueryList | null;
  ssrAPIs: sitePagePluginCreatorSsrApIsQueryList | null;
  pluginFilepath: sitePagePluginCreatorPluginFilepathQueryString | null;
  packageJson: sitePagePluginCreatorPackageJsonInputObject | null;
  internal: sitePagePluginCreatorInternalInputObject | null;
}

export interface sitePagePluginCreatorResolveQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorIdQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorVersionQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsInputObject {
  plugins: sitePagePluginCreatorPluginOptionsPluginsQueryList | null;
  name: sitePagePluginCreatorPluginOptionsNameQueryString | null;
  path: sitePagePluginCreatorPluginOptionsPathQueryString | null;
  pathToConfigModule: sitePagePluginCreatorPluginOptionsPathToConfigModuleQueryString | null;
  blocks: sitePagePluginCreatorPluginOptionsBlocksInputObject | null;
  pathCheck: sitePagePluginCreatorPluginOptionsPathCheckQueryBoolean | null;
}

export interface sitePagePluginCreatorPluginOptionsPluginsQueryList {
  elemMatch: sitePagePluginCreatorPluginOptionsPluginsInputObject | null;
}

export interface sitePagePluginCreatorPluginOptionsPluginsInputObject {
  resolve: sitePagePluginCreatorPluginOptionsPluginsResolveQueryString | null;
  id: sitePagePluginCreatorPluginOptionsPluginsIdQueryString | null;
  name: sitePagePluginCreatorPluginOptionsPluginsNameQueryString | null;
  version: sitePagePluginCreatorPluginOptionsPluginsVersionQueryString | null;
  pluginOptions: sitePagePluginCreatorPluginOptionsPluginsPluginOptionsInputObject | null;
  pluginFilepath: sitePagePluginCreatorPluginOptionsPluginsPluginFilepathQueryString | null;
}

export interface sitePagePluginCreatorPluginOptionsPluginsResolveQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsPluginsIdQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsPluginsNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsPluginsVersionQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsPluginsPluginOptionsInputObject {
  blocks: sitePagePluginCreatorPluginOptionsPluginsPluginOptionsBlocksInputObject | null;
}

export interface sitePagePluginCreatorPluginOptionsPluginsPluginOptionsBlocksInputObject {
  danger: sitePagePluginCreatorPluginOptionsPluginsPluginOptionsBlocksDangerQueryString | null;
  warning: sitePagePluginCreatorPluginOptionsPluginsPluginOptionsBlocksWarningQueryString | null;
  info: sitePagePluginCreatorPluginOptionsPluginsPluginOptionsBlocksInfoQueryString | null;
  success: sitePagePluginCreatorPluginOptionsPluginsPluginOptionsBlocksSuccessQueryString | null;
  collapse: sitePagePluginCreatorPluginOptionsPluginsPluginOptionsBlocksCollapseQueryString | null;
}

export interface sitePagePluginCreatorPluginOptionsPluginsPluginOptionsBlocksDangerQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsPluginsPluginOptionsBlocksWarningQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsPluginsPluginOptionsBlocksInfoQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsPluginsPluginOptionsBlocksSuccessQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsPluginsPluginOptionsBlocksCollapseQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsPluginsPluginFilepathQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsPathQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsPathToConfigModuleQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsBlocksInputObject {
  danger: sitePagePluginCreatorPluginOptionsBlocksDangerQueryString | null;
  warning: sitePagePluginCreatorPluginOptionsBlocksWarningQueryString | null;
  info: sitePagePluginCreatorPluginOptionsBlocksInfoQueryString | null;
  success: sitePagePluginCreatorPluginOptionsBlocksSuccessQueryString | null;
  collapse: sitePagePluginCreatorPluginOptionsBlocksCollapseQueryString | null;
}

export interface sitePagePluginCreatorPluginOptionsBlocksDangerQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsBlocksWarningQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsBlocksInfoQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsBlocksSuccessQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsBlocksCollapseQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginOptionsPathCheckQueryBoolean {
  eq: boolean | null;
  ne: boolean | null;
  in: Array<boolean> | null;
  nin: Array<boolean> | null;
}

export interface sitePagePluginCreatorNodeApIsQueryList {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorBrowserApIsQueryList {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorSsrApIsQueryList {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPluginFilepathQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPackageJsonInputObject {
  name: sitePagePluginCreatorPackageJsonNameQueryString | null;
  description: sitePagePluginCreatorPackageJsonDescriptionQueryString | null;
  version: sitePagePluginCreatorPackageJsonVersionQueryString | null;
  main: sitePagePluginCreatorPackageJsonMainQueryString | null;
  author: sitePagePluginCreatorPackageJsonAuthorQueryString | null;
  license: sitePagePluginCreatorPackageJsonLicenseQueryString | null;
  dependencies: sitePagePluginCreatorPackageJsonDependenciesQueryList | null;
  devDependencies: sitePagePluginCreatorPackageJsonDevDependenciesQueryList | null;
  peerDependencies: sitePagePluginCreatorPackageJsonPeerDependenciesQueryList | null;
  keywords: sitePagePluginCreatorPackageJsonKeywordsQueryList | null;
}

export interface sitePagePluginCreatorPackageJsonNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPackageJsonDescriptionQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPackageJsonVersionQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPackageJsonMainQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPackageJsonAuthorQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPackageJsonLicenseQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPackageJsonDependenciesQueryList {
  elemMatch: sitePagePluginCreatorPackageJsonDependenciesInputObject | null;
}

export interface sitePagePluginCreatorPackageJsonDependenciesInputObject {
  name: sitePagePluginCreatorPackageJsonDependenciesNameQueryString | null;
  version: sitePagePluginCreatorPackageJsonDependenciesVersionQueryString | null;
}

export interface sitePagePluginCreatorPackageJsonDependenciesNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPackageJsonDependenciesVersionQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPackageJsonDevDependenciesQueryList {
  elemMatch: sitePagePluginCreatorPackageJsonDevDependenciesInputObject | null;
}

export interface sitePagePluginCreatorPackageJsonDevDependenciesInputObject {
  name: sitePagePluginCreatorPackageJsonDevDependenciesNameQueryString | null;
  version: sitePagePluginCreatorPackageJsonDevDependenciesVersionQueryString | null;
}

export interface sitePagePluginCreatorPackageJsonDevDependenciesNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPackageJsonDevDependenciesVersionQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPackageJsonPeerDependenciesQueryList {
  elemMatch: sitePagePluginCreatorPackageJsonPeerDependenciesInputObject | null;
}

export interface sitePagePluginCreatorPackageJsonPeerDependenciesInputObject {
  name: sitePagePluginCreatorPackageJsonPeerDependenciesNameQueryString | null;
  version: sitePagePluginCreatorPackageJsonPeerDependenciesVersionQueryString | null;
}

export interface sitePagePluginCreatorPackageJsonPeerDependenciesNameQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPackageJsonPeerDependenciesVersionQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorPackageJsonKeywordsQueryList {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorInternalInputObject {
  contentDigest: sitePagePluginCreatorInternalContentDigestQueryString | null;
  type: sitePagePluginCreatorInternalTypeQueryString | null;
  owner: sitePagePluginCreatorInternalOwnerQueryString | null;
}

export interface sitePagePluginCreatorInternalContentDigestQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorInternalTypeQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorInternalOwnerQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePagePluginCreatorIdQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageComponentPathQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageIdQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageInternalInputObject_2 {
  type: sitePageInternalTypeQueryString_2 | null;
  contentDigest: sitePageInternalContentDigestQueryString_2 | null;
  description: sitePageInternalDescriptionQueryString | null;
  owner: sitePageInternalOwnerQueryString_2 | null;
}

export interface sitePageInternalTypeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageInternalContentDigestQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageInternalDescriptionQueryString {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePageInternalOwnerQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginResolveQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginIdQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginVersionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsInputObject_2 {
  plugins: sitePluginPluginOptionsPluginsQueryList_2 | null;
  name: sitePluginPluginOptionsNameQueryString_2 | null;
  path: sitePluginPluginOptionsPathQueryString_2 | null;
  pathToConfigModule: sitePluginPluginOptionsPathToConfigModuleQueryString_2 | null;
  blocks: sitePluginPluginOptionsBlocksInputObject_2 | null;
  pathCheck: sitePluginPluginOptionsPathCheckQueryBoolean_2 | null;
}

export interface sitePluginPluginOptionsPluginsQueryList_2 {
  elemMatch: sitePluginPluginOptionsPluginsInputObject_2 | null;
}

export interface sitePluginPluginOptionsPluginsInputObject_2 {
  resolve: sitePluginPluginOptionsPluginsResolveQueryString_2 | null;
  id: sitePluginPluginOptionsPluginsIdQueryString_2 | null;
  name: sitePluginPluginOptionsPluginsNameQueryString_2 | null;
  version: sitePluginPluginOptionsPluginsVersionQueryString_2 | null;
  pluginOptions: sitePluginPluginOptionsPluginsPluginOptionsInputObject_2 | null;
  pluginFilepath: sitePluginPluginOptionsPluginsPluginFilepathQueryString_2 | null;
}

export interface sitePluginPluginOptionsPluginsResolveQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsPluginsIdQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsPluginsNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsPluginsVersionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsPluginsPluginOptionsInputObject_2 {
  blocks: sitePluginPluginOptionsPluginsPluginOptionsBlocksInputObject_2 | null;
}

export interface sitePluginPluginOptionsPluginsPluginOptionsBlocksInputObject_2 {
  danger: sitePluginPluginOptionsPluginsPluginOptionsBlocksDangerQueryString_2 | null;
  warning: sitePluginPluginOptionsPluginsPluginOptionsBlocksWarningQueryString_2 | null;
  info: sitePluginPluginOptionsPluginsPluginOptionsBlocksInfoQueryString_2 | null;
  success: sitePluginPluginOptionsPluginsPluginOptionsBlocksSuccessQueryString_2 | null;
  collapse: sitePluginPluginOptionsPluginsPluginOptionsBlocksCollapseQueryString_2 | null;
}

export interface sitePluginPluginOptionsPluginsPluginOptionsBlocksDangerQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsPluginsPluginOptionsBlocksWarningQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsPluginsPluginOptionsBlocksInfoQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsPluginsPluginOptionsBlocksSuccessQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsPluginsPluginOptionsBlocksCollapseQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsPluginsPluginFilepathQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsPathQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsPathToConfigModuleQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsBlocksInputObject_2 {
  danger: sitePluginPluginOptionsBlocksDangerQueryString_2 | null;
  warning: sitePluginPluginOptionsBlocksWarningQueryString_2 | null;
  info: sitePluginPluginOptionsBlocksInfoQueryString_2 | null;
  success: sitePluginPluginOptionsBlocksSuccessQueryString_2 | null;
  collapse: sitePluginPluginOptionsBlocksCollapseQueryString_2 | null;
}

export interface sitePluginPluginOptionsBlocksDangerQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsBlocksWarningQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsBlocksInfoQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsBlocksSuccessQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsBlocksCollapseQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginOptionsPathCheckQueryBoolean_2 {
  eq: boolean | null;
  ne: boolean | null;
  in: Array<boolean> | null;
  nin: Array<boolean> | null;
}

export interface sitePluginNodeApIsQueryList_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginBrowserApIsQueryList_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginSsrApIsQueryList_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPluginFilepathQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPackageJsonInputObject_2 {
  name: sitePluginPackageJsonNameQueryString_2 | null;
  description: sitePluginPackageJsonDescriptionQueryString_2 | null;
  version: sitePluginPackageJsonVersionQueryString_2 | null;
  main: sitePluginPackageJsonMainQueryString_2 | null;
  author: sitePluginPackageJsonAuthorQueryString_2 | null;
  license: sitePluginPackageJsonLicenseQueryString_2 | null;
  dependencies: sitePluginPackageJsonDependenciesQueryList_2 | null;
  devDependencies: sitePluginPackageJsonDevDependenciesQueryList_2 | null;
  peerDependencies: sitePluginPackageJsonPeerDependenciesQueryList_2 | null;
  keywords: sitePluginPackageJsonKeywordsQueryList_2 | null;
}

export interface sitePluginPackageJsonNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPackageJsonDescriptionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPackageJsonVersionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPackageJsonMainQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPackageJsonAuthorQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPackageJsonLicenseQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPackageJsonDependenciesQueryList_2 {
  elemMatch: sitePluginPackageJsonDependenciesInputObject_2 | null;
}

export interface sitePluginPackageJsonDependenciesInputObject_2 {
  name: sitePluginPackageJsonDependenciesNameQueryString_2 | null;
  version: sitePluginPackageJsonDependenciesVersionQueryString_2 | null;
}

export interface sitePluginPackageJsonDependenciesNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPackageJsonDependenciesVersionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPackageJsonDevDependenciesQueryList_2 {
  elemMatch: sitePluginPackageJsonDevDependenciesInputObject_2 | null;
}

export interface sitePluginPackageJsonDevDependenciesInputObject_2 {
  name: sitePluginPackageJsonDevDependenciesNameQueryString_2 | null;
  version: sitePluginPackageJsonDevDependenciesVersionQueryString_2 | null;
}

export interface sitePluginPackageJsonDevDependenciesNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPackageJsonDevDependenciesVersionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPackageJsonPeerDependenciesQueryList_2 {
  elemMatch: sitePluginPackageJsonPeerDependenciesInputObject_2 | null;
}

export interface sitePluginPackageJsonPeerDependenciesInputObject_2 {
  name: sitePluginPackageJsonPeerDependenciesNameQueryString_2 | null;
  version: sitePluginPackageJsonPeerDependenciesVersionQueryString_2 | null;
}

export interface sitePluginPackageJsonPeerDependenciesNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPackageJsonPeerDependenciesVersionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginPackageJsonKeywordsQueryList_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginInternalInputObject_2 {
  contentDigest: sitePluginInternalContentDigestQueryString_2 | null;
  type: sitePluginInternalTypeQueryString_2 | null;
  owner: sitePluginInternalOwnerQueryString_2 | null;
}

export interface sitePluginInternalContentDigestQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginInternalTypeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePluginInternalOwnerQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface siteSiteMetadataInputObject_2 {
  title: siteSiteMetadataTitleQueryString_2 | null;
}

export interface siteSiteMetadataTitleQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePortQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface siteHostQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePathPrefixQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface sitePolyfillQueryBoolean_2 {
  eq: boolean | null;
  ne: boolean | null;
  in: Array<boolean> | null;
  nin: Array<boolean> | null;
}

export interface siteBuildTimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface siteIdQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface siteInternalInputObject_2 {
  contentDigest: siteInternalContentDigestQueryString_2 | null;
  type: siteInternalTypeQueryString_2 | null;
  owner: siteInternalOwnerQueryString_2 | null;
}

export interface siteInternalContentDigestQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface siteInternalTypeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface siteInternalOwnerQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface Site extends Node {
  id: string;
  parent: Node | null;
  children: Array<Node> | null;
  siteMetadata: siteMetadata_2 | null;
  port: Date | null;
  host: string | null;
  pathPrefix: string | null;
  polyfill: boolean | null;
  buildTime: Date | null;
  internal: internal_12 | null;
}

export interface PortSiteArgs {
  formatString: string | null;
  fromNow: boolean | null;
  difference: string | null;
  locale: string | null;
}

export interface BuildTimeSiteArgs {
  formatString: string | null;
  fromNow: boolean | null;
  difference: string | null;
  locale: string | null;
}

export interface siteMetadata_2 {
  title: string | null;
}

export interface internal_12 {
  contentDigest: string | null;
  type: string | null;
  owner: string | null;
}

export interface directoryIdQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryInternalInputObject_2 {
  contentDigest: directoryInternalContentDigestQueryString_2 | null;
  type: directoryInternalTypeQueryString_2 | null;
  description: directoryInternalDescriptionQueryString_2 | null;
  owner: directoryInternalOwnerQueryString_2 | null;
}

export interface directoryInternalContentDigestQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryInternalTypeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryInternalDescriptionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryInternalOwnerQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directorySourceInstanceNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryAbsolutePathQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryRelativePathQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryExtensionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directorySizeQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryPrettySizeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryModifiedTimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryAccessTimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryChangeTimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryBirthTimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryRootQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryDirQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryBaseQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryExtQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryRelativeDirectoryQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryDevQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryModeQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryNlinkQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryUidQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryGidQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryRdevQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryBlksizeQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryInoQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryBlocksQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryAtimeMsQueryFloat_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryMtimeMsQueryFloat_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryCtimeMsQueryFloat_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryBirthtimeMsQueryFloat_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface directoryAtimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryMtimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryCtimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface directoryBirthtimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileIdQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileInternalInputObject_2 {
  contentDigest: fileInternalContentDigestQueryString_2 | null;
  type: fileInternalTypeQueryString_2 | null;
  mediaType: fileInternalMediaTypeQueryString_2 | null;
  description: fileInternalDescriptionQueryString_2 | null;
  owner: fileInternalOwnerQueryString_2 | null;
}

export interface fileInternalContentDigestQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileInternalTypeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileInternalMediaTypeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileInternalDescriptionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileInternalOwnerQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileSourceInstanceNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileAbsolutePathQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileRelativePathQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileExtensionQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileSizeQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface filePrettySizeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileModifiedTimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileAccessTimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileChangeTimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileBirthTimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileRootQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileDirQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileBaseQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileExtQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileNameQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileRelativeDirectoryQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileDevQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileModeQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileNlinkQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileUidQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileGidQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileRdevQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileBlksizeQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileInoQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileBlocksQueryInteger_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileAtimeMsQueryFloat_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileMtimeMsQueryFloat_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileCtimeMsQueryFloat_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileBirthtimeMsQueryFloat_2 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface fileAtimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileMtimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileCtimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface fileBirthtimeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface publicUrlQueryString_3 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkIdQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkInternalInputObject_2 {
  content: markdownRemarkInternalContentQueryString_2 | null;
  type: markdownRemarkInternalTypeQueryString_2 | null;
  contentDigest: markdownRemarkInternalContentDigestQueryString_2 | null;
  owner: markdownRemarkInternalOwnerQueryString_2 | null;
  fieldOwners: markdownRemarkInternalFieldOwnersInputObject_2 | null;
}

export interface markdownRemarkInternalContentQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkInternalTypeQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkInternalContentDigestQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkInternalOwnerQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkInternalFieldOwnersInputObject_2 {
  slug: markdownRemarkInternalFieldOwnersSlugQueryString_2 | null;
}

export interface markdownRemarkInternalFieldOwnersSlugQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkFrontmatterInputObject_2 {
  title: markdownRemarkFrontmatterTitleQueryString_2 | null;
  nav: markdownRemarkFrontmatterNavQueryString_2 | null;
  url: markdownRemarkFrontmatterUrlQueryString_2 | null;
}

export interface markdownRemarkFrontmatterTitleQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkFrontmatterNavQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkFrontmatterUrlQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkRawMarkdownBodyQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkFileAbsolutePathQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface markdownRemarkFieldsInputObject_2 {
  slug: markdownRemarkFieldsSlugQueryString_2 | null;
}

export interface markdownRemarkFieldsSlugQueryString_2 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface htmlQueryString_3 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface excerptQueryString_3 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface headingsQueryList_3 {
  elemMatch: headingsListElemTypeName_3 | null;
}

export interface headingsListElemTypeName_3 {
  value: headingsListElemValueQueryString_3 | null;
  depth: headingsListElemDepthQueryInt_3 | null;
}

export interface headingsListElemValueQueryString_3 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface headingsListElemDepthQueryInt_3 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface timeToReadQueryInt_3 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface tableOfContentsQueryString_3 {
  eq: string | null;
  ne: string | null;
  regex: string | null;
  glob: string | null;
  in: Array<string> | null;
  nin: Array<string> | null;
}

export interface wordCountTypeName_3 {
  paragraphs: wordCountParagraphsQueryInt_3 | null;
  sentences: wordCountSentencesQueryInt_3 | null;
  words: wordCountWordsQueryInt_3 | null;
}

export interface wordCountParagraphsQueryInt_3 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface wordCountSentencesQueryInt_3 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}

export interface wordCountWordsQueryInt_3 {
  eq: number | null;
  ne: number | null;
  gt: number | null;
  gte: number | null;
  lt: number | null;
  lte: number | null;
  in: Array<number> | null;
  nin: Array<number> | null;
}
