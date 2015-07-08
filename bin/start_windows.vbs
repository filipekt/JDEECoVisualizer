Set oShell = CreateObject ("Wscript.Shell") 
Dim strArgs
strArgs = "cmd /c start_windows.bat"
oShell.Run strArgs, 0, false