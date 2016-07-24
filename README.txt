----------To run the application:----------

1) 	Make sure the following software is installed:
	- JDK (Java Development Kit), version 7 or later
	- Apache Ant 1.8.2 or later
	- JavaFX 2.2 or later (included in the Oracle JDK since version 7 update 6 
	by default, for earlier updates of JRE7 it must be installed separately)
	
2)	Make sure that you have the JavaFX library (jfxrt.jar) on the class path. 
	In Oracle JRE8 it is done automatically, but in JRE7 it has to be done manually.
	In that case, just append the full path to jfxrt.jar to the CLASSPATH
	system variable. In addition, for OpenJDK the JavaFX has to be installed
	separately; it is currrenly provided as the OpenJFX project. 

3) 	On Windows, run start_windows.bat , located in the bin/ directory.
	On Unix with bash, run start_unix.sh , located in the bin/ directory.
	The respective scripts will first build and than immediately run the
	application.
	