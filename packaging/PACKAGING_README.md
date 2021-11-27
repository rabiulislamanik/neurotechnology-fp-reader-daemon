# *** Packaging Fingerprint reader daemon for production***

### Prerequisites
* Install Launch4j
* Install InnoSetup
* Download and keep the 'SIVS_DRIVERS' folder in Desktop

### Create 'jar to exe' with Launch4j
* Keep your latest built jar file in desktop directory.
* Launch the launch4j app and open the conf file named 'launch4j_conf_for_fpreader_daemon.xml' in the packaging directory.
* Build the exe by clicking the build icon.
* It will generate an exe file named 'sivs_webbridge.exe' in the desktop folder.

### Build the installer after creating the exe from the jar

* Move and (replace if any previous sivs_webbridge.exe exists) the generated exe file to SIVS_DRIVERS folder.
* Open the sivs_driver_installer.iss file and complile it. It will generate a sivs_driver_installer.exe file in the desktop folder.