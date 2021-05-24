package vn.mk.eid.emrtd.api.data;

import lombok.Data;

@Data
public class BiometricData {
    Integer type; //1: face 2: finger
    Integer quality;
    private byte[] image;
    private byte[] template;
}
