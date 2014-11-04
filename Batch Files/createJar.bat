cd ../Class

del ConnectionDaemon.jar manifest.txt
echo Main-Class: ConnectionDaemon >manifest.txt
jar cvfm ConnectionDaemon.jar manifest.txt *.class

del SimHost.jar manifest.txt
echo Main-Class: SimHost >manifest.txt
jar cvfm SimHost.jar manifest.txt *.class

del SimSwitch.jar manifest.txt
echo Main-Class: SimSwitch >manifest.txt
jar cvfm SimSwitch.jar manifest.txt *.class

del SimRouter.jar manifest.txt
echo Main-Class: SimRouter >manifest.txt
jar cvfm SimRouter.jar manifest.txt *.class

rem rm ../Exec/*.jar
rem mv *.jar ../Exec

del manifest.txt
