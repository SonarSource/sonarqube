---
title: Server Logs & System Info
url: /instance-administration/system-info/
---

The System Info page is found at **[Administration > System](/#sonarqube-admin#/admin/system)**. It gives you access to detailed information on the state of your SonarQube instance. 

## System Info

You can browse details about your running instance on this page. 

### Download

Additionally, if you have a Support contract, you might be asked by a Support representative to send in your System Info, which can be downloaded from the page **[Administration > System](/#sonarqube-admin#/admin/system)** using the **"Download System Info"** button at the top.

### Server Id
Your server id can be obtained from this page by expanding the **System** section. If you're running a commercial instance, you can also find this value on the License page (**[Administration > Configuration > License Manager](/#sonarqube-admin#/admin/extension/license/app)**)

## Logs
Server-side logging is controlled by properties set in _$SONARQUBE-HOME/conf/sonar.properties_.

4 logs files are created: one per SonarQube process.

### Log Level
The server-side log level can be customized via the `sonar.log.level` property. Supported values are:

* **`INFO`** - the default
* **`DEBUG`** - for advanced logs.
* **`TRACE`** - show advanced logs and all SQL and Elasticsearch requests. `TRACE` level logging slows down the server environment, and should be used only for tracking web request performance problems.

### Log Level by Process
The server-side log level can be adjusted more precisely for the 4 processes of SonarQube Server via the following property:

* **`sonar.log.level.app`** - for the Main process of SonarQube (aka WrapperSimpleApp, the bootstrapper process starting the 3 others) 
* **`sonar.log.level.web`** - for the WebServer
* **`sonar.log.level.ce`** - for the ComputeEngineServer
* **`sonar.log.level.es`** - for the SearchServer

### Log Rotation
To control log rolling, use the `sonar.log.rollingPolicy`

* **`time:[value]`** - for time-based rotation. For example, use `time:yyyy-MM-dd` for daily rotation, and * `time:yyyy-MM` for monthly rotation.
* **`size:[value]`** - for size-based rotation. For example, `size:10MB`.
* **`none`** - for no rotation. Typically this would be used when logs are handled by an external system like logrotate.

`sonar.log.maxFiles` is the maximum number of files to keep. This property is ignored if `sonar.log.rollingPolicy=none`.

### UI Access to Logs and Log Levels

The System Info page gives you the ability to download your instance's current log files (log files rotate on a regular basis), and to tune the log level via controls at the top of the page. Changes made here are temporary, and last only until the next time the instance is restarted, at which point the level will be reset to the more permanent value set in _$SONARQUBE-HOME/conf/sonar.properties_. Regardless, if you change your log level _from_ `INFO`, but sure to change it back as soon as is practical; log files can get very large very quickly at lower log levels.

## Total Lines of Code

### SonarQube 6.7 LTS and newer
The number of Lines of Code (for licensing purposes) in an instance can be found in the **System** section of the System Info page on, and on the License page (**[Administration > Configuration > License Manager](/#sonarqube-admin#/admin/extension/license/app)** in commercial editions. 

If you're on a commercial edition and using Branch or PR analysis, rest assured that only lines from the single largest branch in a project are considered for licensing purposes. The Lines of Code in the rest of the branches are ignored.

### Versions older than 6.7
The best approach there is to query the database. The actual query varies based on the version of SonarQube and the database engine. Two queries are provided:

* one query that counts LOCs across *all* projects
& one query that filters out project branches (i.e. projects analysed with the `sonar.branch` parameter). However, this query is accurate only if projects with branches are also analysed once without sonar.branch.

**SonarQube LTS v5.6.x**

[[collapse]]
| ## MySQL
| Global LOCs
| ```
| select sum(pm.value) as global_loc from projects p
| inner join snapshots s on s.project_id = p.id
| inner join project_measures pm on pm.snapshot_id = s.id
| inner join metrics m on m.id=pm.metric_id
| where s.islast = true
| and p.enabled = true
| and p.qualifier = 'TRK'
| and p.scope = 'PRJ'
| and p.copy_resource_id is null
| and pm.person_id is null
| and m.name='ncloc';
| ```
| LOCs without `sonar.branch`
|```
| select sum(pm.value) as loc_without_branch from projects p
| inner join snapshots s on s.project_id = p.id
| inner join project_measures pm on pm.snapshot_id = s.id
| inner join metrics m on m.id=pm.metric_id
| where s.islast = true
| and p.enabled = true
| and p.qualifier = 'TRK'
| and p.scope = 'PRJ'
| and p.copy_resource_id is null
| and pm.person_id is null
| and m.name='ncloc'
| and (
| INSTR(p.kee, ':') = 0 or not exists(
| select * from projects p_root where p_root.kee = SUBSTR(p.kee, 1, LENGTH(p.kee) - INSTR(REVERSE(p.kee), ':'))
| ));
|```

[[collapse]]
| ## PostgreSQL 8.0-9.0
| Global LOCs
| ```
| select sum(pm.value) as global_loc from projects p
| inner join snapshots s on s.project_id = p.id
| inner join project_measures pm on pm.snapshot_id = s.id
| inner join metrics m on m.id=pm.metric_id
| where s.islast = true
| and p.enabled = true
| and p.qualifier = 'TRK'
| and p.scope = 'PRJ'
| and p.copy_resource_id is null
| and pm.person_id is null
| and m.name='ncloc';
| ```
| LOCs without `sonar.branch`
| Not feasible on this specific database

[[collapse]]
| ## PostgreSQL 9.1+
| Global LOCs
| ```
| select sum(pm.value) as global_loc from projects p
| inner join snapshots s on s.project_id = p.id
| inner join project_measures pm on pm.snapshot_id = s.id
| inner join metrics m on m.id=pm.metric_id
| where s.islast = true
| and p.enabled = true
| and p.qualifier = 'TRK'
| and p.scope = 'PRJ'
| and p.copy_resource_id is null
| and pm.person_id is null
| and m.name='ncloc';
| ```
| LOCs without `sonar.branch`
| ```
| select sum(pm.value) as loc_without_branch from projects p
| inner join snapshots s on s.project_id = p.id
| inner join project_measures pm on pm.snapshot_id = s.id
| inner join metrics m on m.id=pm.metric_id
| where s.islast = true
| and p.enabled = true
| and p.qualifier = 'TRK'
| and p.scope = 'PRJ'
| and p.copy_resource_id is null
| and pm.person_id is null
| and m.name='ncloc'
| and (
| POSITION(':' IN p.kee) = 0 or not exists(
| select * from projects p_root where p_root.kee = SUBSTRING(p.kee, 0, LENGTH(p.kee) - POSITION(':' in REVERSE(p.kee)) + 1)
| ));
| ```

[[collapse]]
| ## Oracle
| Global LOCs
| ```
| select sum(pm.value) as global_loc from projects p
| inner join snapshots s on s.project_id = p.id
| inner join project_measures pm on pm.snapshot_id = s.id
| inner join metrics m on m.id=pm.metric_id
| where s.islast = 1
| and p.enabled = 1
| and p.qualifier = 'TRK'
| and p.scope = 'PRJ'
| and p.copy_resource_id is null
| and pm.person_id is null
| and m.name='ncloc';
| ```
| LOCs without `sonar.branch`
| ```
| select sum(pm.value) as loc_without_branch from projects p
| inner join snapshots s on s.project_id = p.id
| inner join project_measures pm on pm.snapshot_id = s.id
| inner join metrics m on m.id=pm.metric_id
| where s.islast = 1
| and p.enabled = 1
| and p.qualifier = 'TRK'
| and p.scope = 'PRJ'
| and p.copy_resource_id is null
| and pm.person_id is null
| and m.name='ncloc'
| and (
| INSTR(p.kee, ':') = 0 or not exists(
| select * from projects p_root where p_root.kee = SUBSTR(p.kee, 0, INSTR(p.kee, ':', -1) - 1)
| ));
| ```

[[collapse]]
| ## Microsoft SQL Server (a.k.a MSSQL)
| Global LOCs
| ```
| select sum(pm.value) as global_loc from projects p
| inner join snapshots s on s.project_id = p.id
| inner join project_measures pm on pm.snapshot_id = s.id
| inner join metrics m on m.id=pm.metric_id
| where s.islast = 1
| and p.enabled = 1
| and p.qualifier = 'TRK'
| and p.scope = 'PRJ'
| and p.copy_resource_id is null
| and pm.person_id is null
| and m.name='ncloc';
| ```
| LOCs without `sonar.branch`
| ```
| select sum(pm.value) as loc_without_branch from projects p
| inner join snapshots s on s.project_id = p.id
| inner join project_measures pm on pm.snapshot_id = s.id
| inner join metrics m on m.id=pm.metric_id
| where s.islast = 1
| and p.enabled = 1
| and p.qualifier = 'TRK'
| and p.scope = 'PRJ'
| and p.copy_resource_id is null
| and pm.person_id is null
| and m.name='ncloc'
| and (
| CHARINDEX(':', p.kee) = 0 or not exists(
| select * from projects p_root where p_root.kee = SUBSTRING(p.kee, 0, LEN(p.kee) - CHARINDEX(':', REVERSE(p.kee)) + 1 )
| ));
| ```

**SonarQube 6.0-6.6**
[[collapse]]
| ## MySQL
| Global LOCs
| ```
| select sum(pm.value) as global_loc from projects p
| inner join snapshots s on s.component_uuid = p.uuid
| inner join project_measures pm on pm.analysis_uuid = s.uuid
| inner join metrics m on m.id=pm.metric_id
| where s.islast = true
| and p.enabled = true
| and p.qualifier = 'TRK'
| and p.scope = 'PRJ'
| and p.copy_component_uuid is null
| and pm.component_uuid = p.uuid
| and pm.person_id is null
| and m.name='ncloc';
| ```
| LOCs without `sonar.branch`
| ```
| select sum(pm.value) as loc_without_branch from projects p
| inner join snapshots s on s.component_uuid = p.uuid
| inner join project_measures pm on pm.analysis_uuid = s.uuid
| inner join metrics m on m.id=pm.metric_id
| where s.islast = true
| and p.enabled = true
| and p.qualifier = 'TRK'
| and p.scope = 'PRJ'
| and p.copy_component_uuid is null
| and pm.component_uuid = p.uuid
| and pm.person_id is null
| and m.name='ncloc'
| and (
| INSTR(p.kee, ':') = 0 or not exists(
| select * from projects p_root where p_root.kee = SUBSTR(p.kee, 1, LENGTH(p.kee) - INSTR(REVERSE(p.kee), ':'))
| ));
| ```

[[collapse]]
| ## PostgreSQL 8.0-9.0
| Global LOCs
| ```
| select sum(pm.value) as global_loc from projects p
| inner join snapshots s on s.component_uuid = p.uuid
| inner join project_measures pm on pm.analysis_uuid = s.uuid
| inner join metrics m on m.id=pm.metric_id
| where s.islast = true
| and p.enabled = true
| and p.qualifier = 'TRK'
| and p.scope = 'PRJ'
| and p.copy_component_uuid is null
| and pm.component_uuid = p.uuid
| and pm.person_id is null
| and m.name='ncloc';
| ```
| LOCs without `sonar.branch`
| Not feasible on this specific database


[[collapse]]
| ## PostgreSQL 9.1+
| Global LOCs
| ```
| select sum(pm.value) as global_loc from projects p
| inner join snapshots s on s.component_uuid = p.uuid
| inner join project_measures pm on pm.analysis_uuid = s.uuid
| inner join metrics m on m.id=pm.metric_id
| where s.islast = true
| and p.enabled = true
| and p.qualifier = 'TRK'
| and p.scope = 'PRJ'
| and p.copy_component_uuid is null
| and pm.component_uuid = p.uuid
| and pm.person_id is null
| and m.name='ncloc';
| ```
| LOCs without `sonar.branch`
| ```
| select sum(pm.value) as loc_without_branch from projects p
| inner join snapshots s on s.component_uuid = p.uuid
| inner join project_measures pm on pm.analysis_uuid = s.uuid
| inner join metrics m on m.id=pm.metric_id
| where s.islast = true
| and p.enabled = true
| and p.qualifier = 'TRK'
| and p.scope = 'PRJ'
| and p.copy_component_uuid is null
| and pm.component_uuid = p.uuid
| and pm.person_id is null
| and m.name='ncloc' and (
| POSITION(':' IN p.kee) = 0 or not exists(
| select * from projects p_root where p_root.kee = SUBSTRING(p.kee, 0, LENGTH(p.kee) - POSITION(':' in REVERSE(p.kee)) + 1)
| ));
| ```


[[collapse]]
| ## Oracle
| Global LOCs
| ```
| select sum(pm.value) as global_loc from projects p
| inner join snapshots s on s.component_uuid = p.uuid
| inner join project_measures pm on pm.analysis_uuid = s.uuid
| inner join metrics m on m.id=pm.metric_id
| where s.islast = 1
| and p.enabled = 1
| and p.qualifier = 'TRK'
| and p.scope = 'PRJ'
| and p.copy_component_uuid is null
| and pm.component_uuid = p.uuid
| and pm.person_id is null
| and m.name='ncloc';
| ```
| LOCs without `sonar.branch`
| ```
| select sum(pm.value) as loc_without_branch from projects p
| inner join snapshots s on s.component_uuid = p.uuid
| inner join project_measures pm on pm.analysis_uuid = s.uuid
| inner join metrics m on m.id=pm.metric_id
| where s.islast = 1
| and p.enabled = 1
| and p.qualifier = 'TRK'
| and p.scope = 'PRJ'
| and p.copy_component_uuid is null
| and pm.component_uuid = p.uuid
| and pm.person_id is null
| and m.name='ncloc' 
| and (
| INSTR(p.kee, ':') = 0 or not exists(
| select * from projects p_root where p_root.kee = SUBSTR(p.kee, 0, INSTR(p.kee, ':', -1) - 1)
| ));
| ```


[[collapse]]
| ## Microsoft SQL Server (a.k.a. MSSQL)
| Global LOCs
| ```
| select sum(pm.value) as global_loc from projects p
| inner join snapshots s on s.component_uuid = p.uuid
| inner join project_measures pm on pm.analysis_uuid = s.uuid
| inner join metrics m on m.id=pm.metric_id
| where s.islast = 1
| and p.enabled = 1
| and p.qualifier = 'TRK'
| and p.scope = 'PRJ'
| and p.copy_component_uuid is null
| and pm.component_uuid = p.uuid
| and pm.person_id is null
| and m.name='ncloc';
| ```
| LOCs without `sonar.branch`
| ```
| select sum(pm.value) as loc_without_branch from projects p
| inner join snapshots s on s.component_uuid = p.uuid
| inner join project_measures pm on pm.analysis_uuid = s.uuid
| inner join metrics m on m.id=pm.metric_id
| where s.islast = 1
| and p.enabled = 1
| and p.qualifier = 'TRK'
| and p.scope = 'PRJ'
| and p.copy_component_uuid is null
| and pm.component_uuid = p.uuid
| and pm.person_id is null
| and m.name='ncloc' 
| and (
| CHARINDEX(':', p.kee) = 0 or not exists(
| select * from projects p_root where p_root.kee = SUBSTRING(p.kee, 0, LEN(p.kee) - CHARINDEX(':', REVERSE(p.kee)) + 1 )
| ));
| ```


