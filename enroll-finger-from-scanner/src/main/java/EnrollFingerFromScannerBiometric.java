import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;

import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NFinger;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.NTemplateSize;
import com.neurotec.biometrics.client.NBiometricClient;
import com.neurotec.devices.NDeviceManager;
import com.neurotec.devices.NDeviceType;
import com.neurotec.devices.NFScanner;
import com.neurotec.devices.NDeviceManager.DeviceCollection;
import com.neurotec.licensing.NLicense;
import com.neurotec.licensing.NLicenseManager;
import com.neurotec.tutorials.util.LibraryManager;
import com.neurotec.tutorials.util.Utils;
import com.neurotec.images.NImage;
import com.neurotec.images.NImageFormat;
import com.neurotec.images.WSQInfo;
import com.neurotec.lang.NThrowable;
import com.neurotec.io.NBuffer;
import com.neurotec.io.NStream;
import java.util.Base64;
import com.neurotec.util.NVersion;
import com.neurotec.biometrics.NBiometricOperation;
import com.neurotec.biometrics.NBiometricTask;

public final class EnrollFingerFromScannerBiometric {
	private NBiometricClient biometricClient = null;
	private final AtomicBoolean currentWorking = new AtomicBoolean(false);

	public EnrollFingerFromScannerBiometric() throws Exception {
		LibraryManager.initLibraryPath();
		// other licenses: FingerClient, FingerFastExtractor
		final String license = "FingerClient";

		NLicenseManager.setTrialMode(false);
	

		biometricClient = new NBiometricClient();
		biometricClient.setUseDeviceManager(true);
		biometricClient.setFingersCalculateNFIQ(true);
		biometricClient.setFingersCalculateNFIQ2(true);
	}

	public void cancelScanner() {
		biometricClient.cancel();
		currentWorking.set(false);
	}

	public AtomicBoolean getCurrentWorking(){
		return currentWorking;
	}

	public FingerPrintDetails scanFingerPrint() throws Exception {
		if ( ! currentWorking.compareAndSet(false, true)) {
			throw new BScannerException("You are already trying to read a fingerprint, try after completing that one", 409);
		}

		NFinger finger = new NFinger();
		NSubject subject = new NSubject();

		try {
			selectScanner();

			subject.getFingers().add(finger);

			System.out.println("Capturing....");
			NBiometricStatus status = biometricClient.capture(subject);
			if (status != NBiometricStatus.OK) {
				System.out.format("Failed to capture: %s\n", status);
				throw new Exception("Failed to capture");
			}
			
			final Image imageFinger = subject.getFingers()
					.get(0)
					.getImage()
					.toImage();
			BufferedImage bufferedImageFinger = toBufferedImage(imageFinger);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ImageIO.write(bufferedImageFinger, "png", outputStream);
			byte[] bytesImageFinger = outputStream.toByteArray();
			System.out.println("Fingerprint image saved successfully...");

			NImage nimage = null;
			WSQInfo info = null;
			NBiometricTask task = null;
			// Create an NImage from file
			nimage = subject.getFingers()
					.get(0)
					.getImage();

			//Create WSQInfo to store bit rate
			info = (WSQInfo) NImageFormat.getWSQ().createInfo(nimage);

			// Set specified bit rate (or default if bit rate was not specified)
			float bitrate = WSQInfo.DEFAULT_BIT_RATE;
			info.setBitRate(bitrate);
			// Save image in WSQ format and bitrate to file
			NBuffer Nbuffer = nimage.save(info);
			byte[] bytesTemplate = Nbuffer.toByteArray();
			
			String imageEncoded = Base64
				.getEncoder()
				.encodeToString(bytesImageFinger);
			String templateEncoded = Base64
				.getEncoder()
        		.encodeToString(bytesTemplate);

			task = biometricClient.createTask(EnumSet.of(NBiometricOperation.ASSESS_QUALITY), subject);

			biometricClient.performTask(task);
			int nfiq = 0;
			if (task.getStatus() == NBiometricStatus.OK) {
				nfiq = subject.getFingers().get(0).getObjects().get(0).getNFIQ(new NVersion(1, 0));
				System.out.format("Finger NFIQ is: %s\n", nfiq);
			}
			else{
				System.out.format("Quality assessment failed: %s\n", task.getStatus());
			}

			return new FingerPrintDetails(imageEncoded, templateEncoded,nfiq);
		} finally {
			if (finger != null) finger.dispose();
			if (subject != null) subject.dispose();
			currentWorking.set(false);
		}
	}

	// public String getWsqFromBMPFile(String filePath){
	// 	File file = new File(filePath);
	// 	NImage nimage = NImage.fromFile(filePath);
	// 	WSQInfo info = null;
  
	// 	//Create WSQInfo to store bit rate
	// 	info = (WSQInfo) NImageFormat.getWSQ().createInfo(nimage);
  
	// 	// Set specified bit rate (or default if bit rate was not specified)
	// 	float bitrate = WSQInfo.DEFAULT_BIT_RATE;
	// 	info.setBitRate(bitrate);
	// 	// Save image in WSQ format and bitrate to file
	// 	NBuffer Nbuffer = nimage.save(info);
	// 	byte[] bytesTemplate = Nbuffer.toByteArray();
		
	// 	return Base64
	// 	  .getEncoder()
	// 		  .encodeToString(bytesTemplate);
	// }

	public static BufferedImage toBufferedImage(Image img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}

		BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

		Graphics2D bGr = bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();

		return bimage;
	}

	private void selectScanner() throws Exception {
		
		NDeviceManager deviceManager = biometricClient.getDeviceManager();

		deviceManager.setDeviceTypes(EnumSet.of(NDeviceType.FINGER_SCANNER));

		deviceManager.initialize();

		DeviceCollection devices = deviceManager.getDevices();

		if (devices.size() > 0) {
			System.out.format("Found %d fingerprint scanner\n", devices.size());
		} else {
			System.out.format("No scanners found\n");
			throw new BScannerException("There are no devices connected to read the fingerprint", 500);
		}

		if (devices.size() > 1) {

			System.out.println("Multiple detected scanners");
			for (int i = 0; i < devices.size(); i++) {
        System.out.format("\t%d. %s\n", i + 1, devices.get(i).getDisplayName());
      }

			System.out.println("\nThe first one will be selected automatically");
		}

		int selectedScanner = 0;
		biometricClient.setFingerScanner((NFScanner) devices.get(selectedScanner));
	}
}
