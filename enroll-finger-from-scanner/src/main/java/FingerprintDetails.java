import lombok.*;

@Value class FingerPrintDetails {
    String imageBytes;
    String templateBytes;
    int nfiq;
}