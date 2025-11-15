package io.fleetcoreplatform.Models;

public class IoTCertContainer {
    private final String privateKey;
    private final String certificatePEM;
    private final String certificateARN;

    public IoTCertContainer(String privateKey, String certificatePEM, String certificateARN) {
        this.privateKey = privateKey;
        this.certificatePEM = certificatePEM;
        this.certificateARN = certificateARN;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getCertificatePEM() {
        return certificatePEM;
    }

    public String getCertificateARN() {
        return certificateARN;
    }
}
