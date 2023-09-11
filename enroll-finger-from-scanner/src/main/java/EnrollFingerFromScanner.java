import static spark.Spark.*;
import org.json.JSONObject;
import java.util.Base64;
import java.util.concurrent.*;
import spark.Filter;
import spark.Request;
import spark.Response;
import java.util.Optional;
import java.util.stream.*;
import java.util.concurrent.atomic.AtomicBoolean;
import com.neurotec.licensing.NLicense;
import com.neurotec.licensing.NLicenseManager;
import java.io.*;
import java.lang.ProcessBuilder;
import spark.utils.IOUtils;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import com.neurotec.images.NImage;
import com.neurotec.images.NImageFormat;
import com.neurotec.images.WSQInfo;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import com.neurotec.io.NBuffer;

public final class EnrollFingerFromScanner {
  public static void main(String[] args) {

    final EnrollFingerFromScannerBiometric enrollFromScanner;


    try {
      enrollFromScanner = new EnrollFingerFromScannerBiometric();
    } catch (Exception e) {
      System.out.println(e.getMessage());
      return;
    }
    
    port(1212);
    after((Filter) (request, response) -> {
        response.header("Access-Control-Allow-Origin", "*");
        response.header("Access-Control-Allow-Methods", "GET,POST");
    });


    get("/ping" ,(req, res) -> {
      return "ok";
    });

    post("/stopscan" ,(req, res) -> {
      AtomicBoolean isScanGoingOn = enrollFromScanner.getCurrentWorking();
      if(isScanGoingOn.get()){
        enrollFromScanner.cancelScanner();
      }
      return "ok";
    });

    post("/fingerprints", (req, res) -> {
      
      res.type("application/json");
      String timeOutParam = req.queryParams("Timeout");
      System.out.println("Timeout param:"+timeOutParam);
      JSONObject jsonResponse = new JSONObject();
      final ScanThread scanThread = new ScanThread(enrollFromScanner);

      try {

        final String license = "FingerClient";

        
        if (!NLicense.obtain("/local", 5000, license)){
          String licenseFilePath = "c:\\DO_NOT_TOUCH_SIVS_DRIVERS\\Win64_x64\\Activation\\Licenses\\FingerClient_Windows.lic";
          File f = new File(licenseFilePath);
          if(f.exists() && !f.isDirectory()) { 
            BufferedReader br = new BufferedReader(new FileReader(licenseFilePath));
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
    
                while (line != null) {
                    sb.append(line);
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }
                String licensestr = sb.toString();
                NLicense.add(licensestr);
            }finally {
                br.close();
            }
          }
        }

        if (!NLicense.obtain("/local", 5000, license)) {
          System.err.format("Could not obtain license: %s%n", license);
          jsonResponse 
          .put("error", "No License");
        }

        else{
          System.err.format("Obtained License for: %s%n", license);
          final FingerPrintDetails fingerPrintDetails = scanFingerPrint(scanThread,Integer.valueOf(timeOutParam));

          jsonResponse 
            .put("data", new JSONObject()
              .put("WSQImage", fingerPrintDetails.getTemplateBytes())
              .put("BMPBase64", fingerPrintDetails.getImageBytes())
              .put("NFIQ", fingerPrintDetails.getNfiq())
          );
        }

      } catch (BScannerException e) {

        jsonResponse 
          .put("error", e.getMessage());
        res.status(e.getCode());

      } catch (TimeoutException e) {

        scanThread.stopScan();
        jsonResponse 
          .put("error", "Timeout waiting for scan");
        res.status(413);

      } catch (Exception e) {
        e.printStackTrace();
        
        Optional<Throwable> rootCause = Stream.iterate(e, Throwable::getCause)
                                      .filter(element -> element.getCause() == null)
                                      .findFirst();
        String rootCauseMessage = rootCause.get().toString() != null ? rootCause.get().toString().split("\\r")[0] :"";
        jsonResponse 
          .put("error", rootCauseMessage);
        res.status(500);

      } finally {

        scanThread.stopThread();

      }

      return jsonResponse.toString();
    });

    
    post("/startActivation", (req, res) -> {
      //ProcessBuilder pb = new ProcessBuilder();
      // pb.command("c:\\DO_NOT_TOUCH_SIVS_DRIVERS\\Win64_x64\\Activation\\ActivationWizard.exe");  
      // pb.directory(new File("c:\\DO_NOT_TOUCH_SIVS_DRIVERS\\Win64_x64\\Activation"));
      // Process p = pb.start();
      //Process p = Runtime.getRuntime().exec("c:\\DO_NOT_TOUCH_SIVS_DRIVERS\\Win64_x64\\Activation\\ActivationWizard.exe", null, new File("c:\\DO_NOT_TOUCH_SIVS_DRIVERS\\Win64_x64\\Activation\\"));
      ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "c:\\DO_NOT_TOUCH_SIVS_DRIVERS\\Win64_x64\\Activation\\ActivationWizard.exe");
      pb.directory(new File("c:\\DO_NOT_TOUCH_SIVS_DRIVERS\\Win64_x64\\Activation"));
      JSONObject json = new JSONObject().put("statusCode",pb.start().waitFor());
      return json.toString();
    });

    post("/getWsqFromBmp", (req, res) -> {
      res.type("application/json");
      JSONObject jsonResponse = new JSONObject();
      
      initLicense();

      final String license = "FingerClient";
      if (!NLicense.obtain("/local", 5000, license)) {
        System.err.format("Could not obtain license: %s%n", license);
        jsonResponse 
        .put("error", "No License");
        return jsonResponse.toString();
      }

      req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("C:/tmp"));
      Part filePart = req.raw().getPart("uploaded_file");

      String WSQBase64;
      try (InputStream inputStream = filePart.getInputStream()) {
          OutputStream outputStream = new FileOutputStream("C:/tmp/" + filePart.getSubmittedFileName());
          IOUtils.copy(inputStream, outputStream);
          outputStream.close();

          File file = new File("C:/tmp/" + filePart.getSubmittedFileName());
          NImage nimage = NImage.fromFile("C:/tmp/" + filePart.getSubmittedFileName());
          WSQInfo info = null;
    
          //Create WSQInfo to store bit rate
          info = (WSQInfo) NImageFormat.getWSQ().createInfo(nimage);
    
          // Set specified bit rate (or default if bit rate was not specified)
          float bitrate = WSQInfo.DEFAULT_BIT_RATE;
          info.setBitRate(bitrate);
          // Save image in WSQ format and bitrate to file
          NBuffer Nbuffer = nimage.save(info);
          byte[] bytesTemplate = Nbuffer.toByteArray();
          
          WSQBase64 = Base64
            .getEncoder()
                .encodeToString(bytesTemplate);

          file.delete();

          jsonResponse 
            .put("WSQImage", WSQBase64);

          return jsonResponse.toString();
      }
      catch(Exception e){
        e.printStackTrace();
      }

      return "ok";
    });
  }

  private static FingerPrintDetails scanFingerPrint(
    ScanThread scanThread,Integer timeOut) throws Exception {
      
      Future<FingerPrintDetails> future = scanThread.start();
      
      FingerPrintDetails fingerPrintDetails = future.get(timeOut, TimeUnit.MILLISECONDS);
      // if internal exception
      if (fingerPrintDetails == null) {
        throw scanThread.getException();
      }
      return fingerPrintDetails;
      // byte[] imageBytes = fingerPrintDetails.getImageBytes();
      // byte[] templateBytes = fingerPrintDetails.getTemplateBytes();
      // return new FingerPrintDetails(imageEncoded, templateEncoded);
  }

  public static void initLicense(){
    final String license = "FingerClient";

    try {
      if (!NLicense.obtain("/local", 5000, license)){
        String licenseFilePath = "c:\\DO_NOT_TOUCH_SIVS_DRIVERS\\Win64_x64\\Activation\\Licenses\\FingerClient_Windows.lic";
        File f = new File(licenseFilePath);
        if(f.exists() && !f.isDirectory()) { 
          BufferedReader br = new BufferedReader(new FileReader(licenseFilePath));
          try {
              StringBuilder sb = new StringBuilder();
              String line = br.readLine();
  
              while (line != null) {
                  sb.append(line);
                  sb.append(System.lineSeparator());
                  line = br.readLine();
              }
              String licensestr = sb.toString();
              NLicense.add(licensestr);
          }finally {
              br.close();
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  
  }
}
