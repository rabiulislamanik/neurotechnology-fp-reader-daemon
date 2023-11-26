# dmp-db-fingerprint-reader-daemon

This project was inspired by this poc (https://github.com/wiringbits/fingerprint-reader-daemon). We have worked on top of this project according to our needs.We recommend you to take a glimpse of this repository first.

## Run
You will require:
- Windows 10 (Don't use VM, please refer to the repository mentioned above for more details )
- Java 8
- Gradle

### Instructions
* To avoid manual copy pasting, we suggest you to get the sivs_driver_installer file from the team. Install it and activate the license. (You can manually paste them yourselves. )
* Download the Bin folder from here ( https://drive.google.com/drive/folders/1sKEH1Ss7DvyMrKb_e7FpcxMBVvufKLIn?usp=drive_link ) and unzip & paste it in  the root of the project
* Now use this command ```gradle run``` to run the project.
* If the port is already being used (as you are using the driver already) , run cmd as administrator and run these commands-
    - ```netstat -ano | findstr :1212``` , get the pid
    - ```taskkill /F /PID the_pid``` , kill the process using the pid

## Get a fingerprint
You can use any http client (like postman, httpie, or even your webapp) to execute the following request `POST localhost:1212/fingerprints`, which will read a fingerprint from the connected device.

Which initializes the fingerprint reader and waits until you capture a fingerprint, once done, it returns the base64-enconded image and it's template, like:

```json
{
    "data": {
        "image": "aabbcc......",
        "template": "aabbccdd...."
    }
}
```

When there is an error, you should get a response like:
```json
{
    "error": "You are already trying to read a fingerprint, try after completing that one"
}
```

The handled errors are:
- Trying to read a fingerprint when there aren't devices available.
- Trying to read a fingerprint when you are already reading another fingerprint.
- Reading a fingerprint timed out, you took too long.

## Packaging

For packaging and distributing, check the packaging_readme.md in the packaging directory
