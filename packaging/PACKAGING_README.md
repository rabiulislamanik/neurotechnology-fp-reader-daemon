# *** Packaging Fingerprint reader daemon for production***

### Prerequisites
* Neurotech fingerprint reader project
* Fingerprint scanner vendor's drivers (Official drivers of futronic,realscan,secugen ... whichever you need. At of today , all production users are using futronic)
* JRE pack ( from here (https://drive.google.com/file/d/1fH-SOLyVb_nNVDu_I6wvpZGLp54Qg3Jm/view?usp=drive_link))
* Java 8 (8.0.392-amzn)
* Gradle 6.5.1
* Launch4j
* Inno Setup Compiler


### Create 'jar to exe' with Launch4j
* Run gradle customFatJar at the root directory. This will create a jar file named ```all-in-one-jar-11.1.0.0.jar``` , in the 
```neurotechnology-fp-reader-daemon//utils/build/libs``` folder
* Copy the created jar file to the Desktop folder
* Open Launch4j application & add configuration file from following location:
```neurotechnology-fp-reader-daemon/packaging /launch4j_conf_for_fpreader_daemon_YOUR_USER_NAME.xml)```
* Build the project then the ```sivs_webbridge.exe``` will be generated at the Desktop

### Build the installer after creating the exe from the jar

* Dowload this folder named ```SIVS_DRIVERS``` from here (https://drive.google.com/file/d/1dXObV02uLDRSv_ipT06fYIy_pbo9ws7j/view?usp=drive_link)
* Paste the newly created  ```sivs_webbridge.exe``` into the folder. (Replace the existing one if there's any)
* Open Inno Setup Compiler & add the .iss file located at:
```neurotechnology-fp-reader-daemon/packaging/sivs_driver_installer_YOUR_USER_NAME.iss```
* Compile the .iss file and the installer file ```sivs_driver_installer.exe``` will be generated at the Desktop folder
