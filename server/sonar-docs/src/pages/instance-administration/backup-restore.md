---
title: Backup and Restore
url: /instance-administration/backup-restore/
---

## Backing Up Data
Most databases come with backup tools. We recommend using these tools to back up your data.

## Restoring Data
To restore data from backup, follow these steps:

1. Stop the server.
1. Restore the backup.
1. Drop the Elasticsearch indexes by deleting the contents of `$SQ_HOME/data/es7 directory`.
1. Restart the server.
