; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

#define MyAppName "sivs_drivers"
#define MyAppVersion "1.0"
#define MyAppPublisher "Dynamic Solution Innovators ltd"
#define MyAppURL "https://www.example.com/"

[Setup]
; NOTE: The value of AppId uniquely identifies this application. Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{B4DACFFC-1F9B-4654-A61E-D45830B4562C}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName=c:\DO_NOT_TOUCH_SIVS_DRIVERS
DisableDirPage=yes
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
; Uncomment the following line to run in non administrative install mode (install for current user only.)
;PrivilegesRequired=lowest
OutputDir=C:\Users\anik\Desktop
OutputBaseFilename=sivs_driver_installer
Compression=lzma
SolidCompression=yes
WizardStyle=modern


[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Code]
function System32FileExists(FileName: string): Boolean;
var
  OldState: Boolean;
begin
  if IsWin64 then
  begin
    Log('64-bit system');
    OldState := EnableFsRedirection(False);
    if OldState then Log('Disabled WOW64 file system redirection');
    try
      Result := FileExists(FileName);
    finally
      EnableFsRedirection(OldState);
      if OldState then Log('Resumed WOW64 file system redirection');
    end;
  end
    else
  begin
    Log('32-bit system');
    Result := FileExists(FileName);
  end;

  if Result then
    Log(Format('File %s exists', [FileName]))
  else
    Log(Format('File %s does not exists', [FileName]));
end;

[Files]
Source: "C:\Users\anik\Desktop\SIVS_DRIVERS\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Run]
Filename: {app}\RealScan_Driver(x86)-V1.1.0.0.exe;WorkingDir: {app}; Check: (not IsWin64())  and (not FileExists(ExpandConstant('{sys}\drivers\RealScan.sys'))) ; Flags: 32bit;
Filename: {app}\RealScan_Driver(x64)-V1.1.0.0.exe;WorkingDir: {app}; Check: (IsWin64()) and (not System32FileExists(ExpandConstant('{sys}\drivers\RealScan.sys'))); Flags: 64bit;
//Filename: {app}\Win32_x86\Activation\ActivationWizard.exe;WorkingDir: {app};Check: not IsWin64(); Flags: 32bit;
//Filename: {app}\Win64_x64\Activation\ActivationWizard.exe;WorkingDir: {app};Check: IsWin64(); Flags: 64bit;
//Filename: "{cmd}"; Parameters: "/c ""sc.exe create FingerprintService binPath= {app}\sivs_webbridge.exe start= auto""" ;WorkingDir: {app};
Filename: {sys}\sc.exe; Parameters: "create FingerprintService start= auto binPath= ""{app}\sivs_webbridge.exe""" ; Flags: runhidden
Filename: {sys}\sc.exe; Parameters: "start FingerprintService" ; Flags: runhidden
//Filename: "{cmd}"; Parameters: "/c ""sc.exe start FingerprintService""" ;WorkingDir: {app};
