# Diff from the official schema

- Remove all `default` from the schema. This is mostly to fix [Issue #105](https://github.com/oasis-tcs/sarif-spec/issues/105)  but also to prevent all indexes and ids to have -1 as default value, while null is better in Java.