---
title: Managing Project History
url: /project-administration/managing-project-history/
---

One of the most powerful features of {instance} is that it shows you not just your project health today, but how it has changed over time. It does that by selectively keeping data from previous analyses (see [Housekeeping](/instance-administration/housekeeping/)). It doesn't keep all previous analyses - that would bloat the database. Similarly, for the analyses it does keep, {instance} doesn't keep all the data. Once a project snapshot moves from the "Last analysis" (i.e. the most recent) to being part of the project's history, data below the project level is purged - again to keep from bloating the database.

Typically these aren't things you need to even think about; {instance} just handles them for you. But occasionally you may need to remove a bad snapshot from a project's history or change the housekeeping algorithms.

## Managing History
Occasionally, you may need to manually delete a project snapshot, whether because the wrong quality profile was used, or because there was a problem with analysis, and so on. Note that the most recent snapshot (labeled "Last snapshot") can never be deleted.

[[warning]]
|**About deleting snapshots**<br/><br/>
|Deleting a snapshot is a 2-step process:<br/><br/>
|* The snapshot must first be removed from the project history by clicking on Delete snapshot. It won't be displayed anymore on this History page but will still be present in the database.
|* The snapshot is actually deleted during the next project analysis.

At project level, from the front page **Activity** list, choose **Show More** to see the full activity list.

For every snapshot, it is possible to manually:

* Add, rename or remove a version
* Add, rename or remove an event
* Delete the snapshot
