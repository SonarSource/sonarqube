$ErrorActionPreference = "Stop"

function CheckLastExitCode
{
    param ([int[]]$SuccessCodes = @(0))

    if ($SuccessCodes -notcontains $LastExitCode)
	{
        $msg = @"
EXE RETURNED EXIT CODE $LastExitCode
CALLSTACK:$(Get-PSCallStack | Out-String)
"@
        throw $msg
    }
}

switch ($env:RUN)
{
	"ci"
	{
		mvn package "--batch-mode" "-B" "-e" "-V"
		CheckLastExitCode
	}

	default
	{
		throw "Unexpected test mode: ""$env:RUN"""
	}
}
