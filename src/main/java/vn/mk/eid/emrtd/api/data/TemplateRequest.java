package vn.mk.eid.emrtd.api.data;

import lombok.Data;

@Data
public class TemplateRequest {
    private byte[] image;
    //MoC type: 1: face, #1: finger
    private int mocType;
}
