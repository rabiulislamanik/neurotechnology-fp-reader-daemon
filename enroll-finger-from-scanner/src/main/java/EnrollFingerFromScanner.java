import static spark.Spark.*;
import org.json.JSONObject;
import java.util.Base64;
import java.util.concurrent.*;
import spark.Filter;
import spark.Request;
import spark.Response;

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
        response.header("Access-Control-Allow-Methods", "GET");
    });

    post("/fingerprints", (req, res) -> {
      
      res.type("application/json");
      
      JSONObject jsonResponse = new JSONObject();
      final ScanThread scanThread = new ScanThread(enrollFromScanner);

      try {

        final FingerPrintDetails fingerPrintDetails = scanFingerPrint(scanThread);

        jsonResponse 
          .put("data", new JSONObject()
            .put("WSQImage", fingerPrintDetails.getTemplateBytes())
            .put("BMPBase64", fingerPrintDetails.getImageBytes())
            .put("NFIQ", fingerPrintDetails.getNfiq())
        );

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

        jsonResponse 
          .put("error", "Unknown error: [" + e.getMessage() + "]");
        res.status(500);

      } finally {

        scanThread.stopThread();

      }

      return jsonResponse.toString();
    });
  }

  private static FingerPrintDetails scanFingerPrint(
    ScanThread scanThread) throws Exception {
      
      Future<FingerPrintDetails> future = scanThread.start();
      
      FingerPrintDetails fingerPrintDetails = future.get(10000, TimeUnit.MILLISECONDS);
      // if internal exception
      if (fingerPrintDetails == null) {
        throw scanThread.getException();
      }
      return fingerPrintDetails;
      // byte[] imageBytes = fingerPrintDetails.getImageBytes();
      // byte[] templateBytes = fingerPrintDetails.getTemplateBytes();
      // return new FingerPrintDetails(imageEncoded, templateEncoded);
  }
}
