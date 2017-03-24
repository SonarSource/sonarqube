// flow-typed signature: cf6332fcf9a3398cffb131f7da90662b
// flow-typed version: dc0ded3d57/classnames_v2.x.x/flow_>=v0.28.x

type $npm$classnames$Classes =
  string |
  {[className: string]: ?boolean } |
  Array<string> |
  false |
  void |
  null

declare module 'classnames' {
  declare function exports(
    ...classes: Array<$npm$classnames$Classes>
  ): string;
}
