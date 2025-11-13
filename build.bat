call updatePlugin.bat
rem set JAVA_HOME = "C:\Program Files\Java\jdk-21"
del platforms\android\app\build\outputs\apk\debug\app-debug.apk

npx ionic cordova build android --debug 
rem --verbose