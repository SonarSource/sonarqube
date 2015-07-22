VERSION 1.0 CLASS
BEGIN
  MultiUse = -1  'True
  Persistable = 0  'NotPersistable
  DataBindingBehavior = 0  'vbNone
  DataSourceBehavior  = 0  'vbNone
  MTSTransactionMode  = 0  'NotAnMTSObject
END
Attribute VB_Name = "Registry"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = True
Attribute VB_PredeclaredId = False
Attribute VB_Exposed = False
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

Public Enum RegistryHKeyConstants
    HKEY_CLASSES_ROOT = &H80000000
    HKEY_CURRENT_USER = &H80000001
    HKEY_LOCAL_MACHINE = &H80000002
    HKEY_USERS = &H80000003
    HKEY_PERFORMANCE_DATA = &H80000004
    HKEY_CURRENT_CONFIG = &H80000005
    HKEY_DYN_DATA = &H80000006
End Enum

Public Enum RegistryTypeConstants
    REG_NONE = (0)                         'No value type
    REG_SZ = (1)                           'Unicode nul terminated string
'    REG_EXPAND_SZ = (2)                    'Unicode nul terminated string w/enviornment var
'    REG_BINARY = (3)                       'Free form binary
    REG_DWORD = (4)                        '32-bit number
'    REG_DWORD_LITTLE_ENDIAN = (4)          '32-bit number (same as REG_DWORD)
'    REG_DWORD_BIG_ENDIAN = (5)             '32-bit number
'    REG_LINK = (6)                         'Symbolic Link (unicode)
'    REG_MULTI_SZ = (7)                     'Multiple Unicode strings
'    REG_RESOURCE_LIST = (8)                'Resource list in the resource map
'    REG_FULL_RESOURCE_DESCRIPTOR = (9)     'Resource list in the hardware description
'    REG_RESOURCE_REQUIREMENTS_LIST = (10)
End Enum

Public Enum RegistryAccessConstants
    KEY_QUERY_VALUE = &H1
    KEY_SET_VALUE = &H2
    KEY_CREATE_SUB_KEY = &H4
    KEY_ENUMERATE_SUB_KEYS = &H8
    KEY_NOTIFY = &H10
    KEY_CREATE_LINK = &H20
    KEY_ALL_ACCESS = &H3F
End Enum

Public Enum RegistryErrorConstants
    ERROR_SUCCESS = 0
    ERROR_BADKEY = 2
    ERROR_OUTOFMEMORY = 6
    ERROR_MORE_DATA = 234
    ERROR_NO_MORE_ITEMS = 259
End Enum

Public Enum RegistryVolatileConstants
    REG_OPTION_NON_VOLATILE = 0&
    REG_OPTION_VOLATILE = &H1
End Enum

Public Enum RegistryDispositionConstants
    REG_CREATED_NEW_KEY = &H1
    REG_OPENED_EXISTING_KEY = &H2
End Enum

Private oKeys As Keys

Private bShowErrors As Boolean
Private bRaiseErrors As Boolean
'
' Public Properties
'
Public Property Get Keys() As Keys
    If oKeys Is Nothing Then
        Set oKeys = New Keys
        With oKeys
            Set .Registry = Me
            Set .Parent = Me
            .Root = True
        End With
    End If
    Set Keys = oKeys
End Property

Public Property Get ShowErrors() As Boolean
    ShowErrors = bShowErrors
End Property
Public Property Let ShowErrors(ByVal NewVal As Boolean)
    bShowErrors = NewVal
End Property

Public Property Get RaiseErrors() As Boolean
    RaiseErrors = bRaiseErrors
End Property
Public Property Let RaiseErrors(ByVal NewVal As Boolean)
    bRaiseErrors = NewVal
End Property
'
' Public Sub/Function
'
' Base Functions
'
Public Function OpenKey(ByVal hKey As RegistryHKeyConstants, _
                        ByVal Path As String, _
                        ByVal Access As RegistryAccessConstants, _
                        Key As Long) As Boolean
                        
    Dim lRC As Long
    
    OpenKey = False
    
    lRC = RegOpenKeyEx(hKey, Path, 0&, Access, Key)
    If lRC = ERROR_SUCCESS Then
        OpenKey = True
    Else
        HandleError lRC, Path
    End If
End Function

Public Function CreateKey(ByVal hKey As RegistryHKeyConstants, _
                          ByVal Path As String, _
                          ByVal Volatile As RegistryVolatileConstants, _
                          ByVal Access As RegistryAccessConstants, _
                          Key As Long, _
                          Disposition As Long) As Boolean

    Dim lRC As Long
    Dim saKey As SECURITY_ATTRIBUTES
    
    CreateKey = False
    
    lRC = RegCreateKeyEx(hKey, Path, 0, "", Volatile, Access, saKey, Key, Disposition)
    If lRC = ERROR_SUCCESS Then
        CreateKey = True
    Else
        HandleError lRC, Path
    End If
End Function

Public Function DeleteKey(ByVal hKey As RegistryHKeyConstants, _
                          ByVal Path As String) As Boolean

    Dim lRC As Long
        
    DeleteKey = False
    
    lRC = RegDeleteKey(hKey, Path)
    If lRC = ERROR_SUCCESS Then
        DeleteKey = True
    Else
        HandleError lRC, Path
    End If
End Function

Public Function CloseKey(ByVal Path, _
                         Key As Long) As Boolean

    Dim lRC As Long
        
    CloseKey = False
    
    lRC = RegCloseKey(Key)
    If lRC = ERROR_SUCCESS Then
        Key = 0
        CloseKey = True
    Else
        HandleError lRC, Path
    End If
End Function

Public Function QueryValueNull(ByVal hKey As Long, _
                               ByVal Name As String, _
                               ValueType As RegistryTypeConstants, _
                               ValueLen As Long) As Boolean
    
    Dim lRC As Long

    QueryValueNull = False
    
    lRC = RegQueryValueExNull(hKey, Name, 0&, ValueType, 0&, ValueLen)
    If lRC = ERROR_SUCCESS Then
        QueryValueNull = True
    Else
        HandleError lRC, Name
    End If
End Function

Public Function QueryValueString(ByVal hKey As Long, _
                                 ByVal Name As String, _
                                 Value As String, _
                                 ValueLen As Long) As Boolean
    
    Dim lRC As Long

    QueryValueString = False
    
    Value = String(ValueLen, 0)
    
    lRC = RegQueryValueExString(hKey, Name, 0&, REG_SZ, Value, ValueLen)
    If lRC = ERROR_SUCCESS Then
        Value = Left(Value, ValueLen - 1)
        QueryValueString = True
    Else
        HandleError lRC, Name
    End If
End Function

Public Function QueryValueLong(ByVal hKey As Long, _
                               ByVal Name As String, _
                               Value As Long) As Boolean
    
    Dim lRC As Long
    Dim lValueLen As Long
    
    QueryValueLong = False
    
    Value = 0
    
    lRC = RegQueryValueExLong(hKey, Name, 0&, REG_DWORD, Value, 4)
    If lRC = ERROR_SUCCESS Then
        QueryValueLong = True
    Else
        HandleError lRC, Name
    End If
End Function

Public Function SetValueString(ByVal hKey As Long, _
                               ByVal Name As String, _
                               ByVal Value As String) As Boolean

    Dim lRC As Long
            
    SetValueString = False
    
    Value = Value & Chr(0)
    
    lRC = RegSetValueExString(hKey, Name, 0&, REG_SZ, Value, Len(Value))
    If lRC = ERROR_SUCCESS Then
        SetValueString = True
    Else
        HandleError lRC, Name
    End If
End Function

Public Function SetValueLong(ByVal hKey As Long, _
                             ByVal Name As String, _
                             ByVal Value As Long) As Boolean

    Dim lRC As Long
            
    SetValueLong = False
    
    lRC = RegSetValueExLong(hKey, Name, 0&, REG_DWORD, Value, 4)
    If lRC = ERROR_SUCCESS Then
        SetValueLong = True
    Else
        HandleError lRC, Name
    End If
End Function

Public Function DeleteValue(ByVal hKey As Long, _
                            ByVal Name As String) As Boolean

    Dim lRC As Long

    DeleteValue = False
    
    lRC = RegDeleteValue(hKey, Name)
    If lRC = ERROR_SUCCESS Then
        DeleteValue = True
    Else
        HandleError lRC, Name
    End If
End Function
'
'
'
Public Function Check(ByVal WithSubKeys As Boolean, _
                      ByVal WithValues As Boolean) As Boolean
                      
    Dim oKey As Key
    
    Check = False
    
    For Each oKey In Keys
        If Not oKey.Check(WithSubKeys, WithValues) Then
            Exit Function
        End If
    Next
        
    Check = True
End Function

Public Function Create(ByVal WithSubKeys As Boolean, _
                       ByVal WithValues As Boolean) As Boolean

    Dim oKey As Key
    
    Create = False
    
    For Each oKey In Keys
        If Not oKey.Create(WithSubKeys, WithValues) Then
            Exit Function
        End If
    Next
        
    Create = True
End Function

Public Function QueryValues(ByVal WithSubKeys As Boolean) As Boolean
                       
    Dim oKey As Key
    
    QueryValues = False
    
    For Each oKey In Keys
        If Not oKey.QueryValues(WithSubKeys) Then
            Exit Function
        End If
    Next
        
    QueryValues = True
End Function

Public Function SetValues(ByVal WithSubKeys As Boolean) As Boolean
                       
    Dim oKey As Key
    
    SetValues = False
    
    For Each oKey In Keys
        If Not oKey.SetValues(WithSubKeys) Then
            Exit Function
        End If
    Next
        
    SetValues = True
End Function

Public Function EnumKeys(ByVal WithSubKeys As Boolean, _
                         ByVal WithValues As Boolean) As Boolean

    Dim oKey As Key
    
    EnumKeys = False
    
    For Each oKey In Keys
        If Not oKey.EnumKeys(WithSubKeys, WithValues) Then
            Exit Function
        End If
    Next
        
    EnumKeys = True
End Function

Public Function FindKeyByPath(ByVal WithSubKeys As Boolean, _
                              ByVal FindPath As String) As Key
    Dim oKey As Key
    
    Set FindKeyByPath = Nothing
    
    For Each oKey In Keys
        If FindPath = oKey.Path Then
            Set FindKeyByPath = oKey
            Exit Function
        End If
        If WithSubKeys Then
            Set FindKeyByPath = oKey.FindKeyByPath(WithSubKeys, FindPath)
        End If
    Next
End Function

Friend Sub HandleError(ByVal RC As Long, ByVal Text As String)
    Dim sMsg As String
    
    If bShowErrors Then
        sMsg = "Error: " & ErrorText(RC) & ". " & Text
        MsgBox sMsg, vbExclamation
    End If
End Sub
'
' Private Sub/Function
'
Private Sub Class_Initialize()
    'Debug.Print "INIT Registry"
    Set oKeys = Nothing
    bShowErrors = True
    bRaiseErrors = False
End Sub

Private Sub Class_Terminate()
    'Debug.Print "TERM Registry"
End Sub

Private Function ErrorText(ByVal lRC As Long) As String
    Dim s As String
    Select Case lRC
        Case ERROR_BADKEY:          s = "Bad key"
        Case ERROR_MORE_DATA:       s = "More data"
        Case ERROR_OUTOFMEMORY:     s = "Out of memory"
        Case ERROR_NO_MORE_ITEMS:   s = "No more items"
        Case Else:                  s = "RC=" & CStr(lRC)
    End Select
    ErrorText = s
End Function

