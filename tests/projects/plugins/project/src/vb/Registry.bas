Attribute VB_Name = "modRegistry"
' --- GPL ---
'
' Copyright (C) 1999 SAP AG
'
' This program is free software; you can redistribute it and/or
' modify it under the terms of the GNU General Public License
' as published by the Free Software Foundation; either version 2
' of the License, or (at your option) any later version.
'
' This program is distributed in the hope that it will be useful,
' but WITHOUT ANY WARRANTY; without even the implied warranty of
' MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
' GNU General Public License for more details.
'
' You should have received a copy of the GNU General Public License
' along with this program; if not, write to the Free Software
' Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
'
' --- GPL ---
Option Explicit

'Structures Needed For Registry Prototypes
Public Type SECURITY_ATTRIBUTES
    nLength As Long
    lpSecurityDescriptor As Long
    bInheritHandle As Boolean
End Type

Public Type FILETIME
    dwLowDateTime As Long
    dwHighDateTime As Long
End Type

'Registry Function Prototypes
Public Declare Function RegOpenKeyEx Lib "advapi32" Alias "RegOpenKeyExA" ( _
    ByVal hKey As Long, _
    ByVal lpSubKey As String, _
    ByVal ulOptions As Long, _
    ByVal samDesired As Long, _
    phkResult As Long) As Long

Public Declare Function RegCreateKeyEx Lib "advapi32" Alias "RegCreateKeyExA" ( _
    ByVal hKey As Long, _
    ByVal lpSubKey As String, _
    ByVal Reserved As Long, _
    ByVal lpClass As String, _
    ByVal dwOptions As Long, _
    ByVal samDesired As Long, _
    lpSecurityAttributes As SECURITY_ATTRIBUTES, _
    phkResult As Long, _
    lpdwDisposition As Long) As Long

Public Declare Function RegQueryValueExNull Lib "advapi32.dll" Alias "RegQueryValueExA" ( _
    ByVal hKey As Long, _
    ByVal lpValueName As String, _
    ByVal lpReserved As Long, _
    lpType As Long, _
    ByVal lpData As Long, _
    lpcbData As Long) As Long

Public Declare Function RegQueryValueExString Lib "advapi32.dll" Alias "RegQueryValueExA" ( _
    ByVal hKey As Long, _
    ByVal lpValueName As String, _
    ByVal lpReserved As Long, _
    lpType As Long, _
    ByVal lpData As String, _
    lpcbData As Long) As Long
    
Public Declare Function RegQueryValueExLong Lib "advapi32.dll" Alias "RegQueryValueExA" ( _
    ByVal hKey As Long, _
    ByVal lpValueName As String, _
    ByVal lpReserved As Long, _
    lpType As Long, _
    lpData As Long, _
    lpcbData As Long) As Long

Public Declare Function RegSetValueExString Lib "advapi32.dll" Alias "RegSetValueExA" ( _
    ByVal hKey As Long, _
    ByVal lpValueName As String, _
    ByVal Reserved As Long, _
    ByVal dwType As Long, _
    ByVal lpValue As String, _
    ByVal cbData As Long) As Long
    
Public Declare Function RegSetValueExLong Lib "advapi32.dll" Alias "RegSetValueExA" ( _
    ByVal hKey As Long, _
    ByVal lpValueName As String, _
    ByVal Reserved As Long, _
    ByVal dwType As Long, _
    lpValue As Long, _
    ByVal cbData As Long) As Long

Public Declare Function RegEnumKeyEx Lib "advapi32.dll" Alias "RegEnumKeyExA" ( _
    ByVal hKey As Long, _
    ByVal dwIndex As Long, _
    ByVal lpName As String, _
    lpcbName As Long, _
    ByVal lpReserved As Long, _
    ByVal lpClass As String, _
    lpcbClass As Long, _
    lpftLastWriteTime As FILETIME) As Long

Public Declare Function RegEnumValue Lib "advapi32.dll" Alias "RegEnumValueA" ( _
    ByVal hKey As Long, _
    ByVal dwIndex As Long, _
    ByVal lpValueName As String, _
    lpcbValueName As Long, _
    ByVal lpReserved As Long, _
    lpType As Long, _
    lpData As Any, _
    lpcbData As Long) As Long

Public Declare Function RegDeleteKey Lib "advapi32.dll" Alias "RegDeleteKeyA" ( _
    ByVal hKey As Long, _
    ByVal lpSubKey As String) As Long

Public Declare Function RegDeleteValue Lib "advapi32.dll" Alias "RegDeleteValueA" ( _
    ByVal hKey As Long, _
    ByVal lpValueName As String) As Long

Public Declare Function RegCloseKey Lib "advapi32" ( _
    ByVal hKey As Long) As Long

'
''masks for the predefined standard access types
'Private Const STANDARD_RIGHTS_ALL = &H1F0000
'Private Const SPECIFIC_RIGHTS_ALL = &HFFFF
'
''Define severity codes
'
''Public Const ERROR_ACCESS_DENIED = 5
''
''Global Const ERROR_NONE = 0
''Global Const ERROR_BADDB = 1
''Global Const ERROR_CANTOPEN = 3
''Global Const ERROR_CANTREAD = 4
''Global Const ERROR_CANTWRITE = 5
''Global Const ERROR_OUTOFMEMORY = 6
''Global Const ERROR_INVALID_PARAMETER = 7
''Global Const ERROR_ACCESS_DENIED = 8
''Global Const ERROR_INVALID_PARAMETERS = 87
''Global Const ERROR_NO_MORE_ITEMS = 259

Public Type ByteValue
    b(1024) As Byte
End Type

Public Type LongValue
    l As Long
End Type

Public Function BytesToString(bValue As ByteValue) As String
    Dim s As String
    Dim i As Integer
    s = StrConv(bValue.b(), vbUnicode)
    i = InStr(s, Chr(0)) - 1
    BytesToString = Left(s, i)
End Function

Public Function BytesToLong(bValue As ByteValue) As Long
    Dim lValue As LongValue
    LSet lValue = bValue
    BytesToLong = lValue.l
End Function

