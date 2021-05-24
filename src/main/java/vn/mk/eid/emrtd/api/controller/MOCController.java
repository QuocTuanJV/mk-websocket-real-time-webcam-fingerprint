package vn.mk.eid.emrtd.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.mk.eid.emrtd.api.data.TemplateRequest;
import vn.mk.eid.emrtd.api.service.DeviceService;
import vn.mk.eid.emrtd.common.data.ResultCode;
import vn.mk.eid.emrtd.common.data.ServiceResult;
import vn.mk.eid.emrtd.common.moc.MocRequest;
import vn.mk.eid.emrtd.common.web.BaseCardRequest;
import vn.mk.eid.emrtd.icao.service.EmrtdService;
import vn.mk.eid.emrtd.moc.biometric.ImageProcessing;
import vn.mk.eid.emrtd.moc.biometric.MocService;

@RestController
@RequestMapping("/moc")
public class MOCController {
    EmrtdService emrtdService;
    ImageProcessing imageProcessing;

    @Value("${readerlist.samreaders}")
    private String samreaders;

    @Autowired
    private MocService mocService;
    @Autowired
    private DeviceService deviceService;

    @Autowired
    public void setEmrtdService(EmrtdService emrtdService) {
        this.emrtdService = emrtdService;
    }

    @PostMapping("/verify")
    public ServiceResult MOCVerify(@RequestBody MocRequest mocRequest) {
        try {
            //check sam
            String samreader = deviceService.getSamreader();
            if (samreader.equals("notregistered"))
                return ServiceResult.fail(ResultCode.READER_NOT_REGISTERED);
            mocRequest.setSamReader(samreader);

            // check card
            String reader = deviceService.getReader();
            if (reader.equals(""))
                return ServiceResult.fail(ResultCode.READER_NOT_FOUND);
            mocRequest.setCardReader(reader);

            BaseCardRequest request = new BaseCardRequest();
            request.setReader(reader);
            boolean isCard = emrtdService.checkCardPresent(request).getSuccess();
            if (!isCard) {
                return ServiceResult.fail(ResultCode.CARD_NOT_FOUND);
            }

            if (mocRequest.getMocType() == 1) //face
            {
//                return mocService.doMocFromTemplate(mocRequest);
                return mocService.doMoc(mocRequest);
            } else // finger
            {
                mocRequest.setMocType(2); //finger 1
                ServiceResult sr = mocService.doMoc(mocRequest);
                if (sr.getMessage().equals(ResultCode.VERIFY_MOC_FAILED.getDescription())) {
                    mocRequest.setMocType(3); //finger 2
                    sr = mocService.doMoc(mocRequest);
                }
                return sr;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ServiceResult.fail(ResultCode.VERIFY_MOC_ERROR);
    }

    @PostMapping("/2template")
    public ServiceResult CreateTemplate(@RequestBody TemplateRequest request) {
        try {
            byte[] tplBytes;
            if (request.getMocType() == 1) // face
                tplBytes = imageProcessing.createFaceTemplateForCard(request.getImage());
            else //finger
                tplBytes = imageProcessing.createFingerTemplateForCard(request.getImage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ServiceResult.fail(ResultCode.CREATE_TEMPLATE_ERROR);
    }

    private byte[] String2Byte(String str) {
        byte[] val = new byte[str.length() / 2];
        for (int i = 0; i < val.length; i++) {
            int index = i * 2;
            int j = Integer.parseInt(str.substring(index, index + 2), 16);
            val[i] = (byte) j;
        }
        return val;
    }
}
