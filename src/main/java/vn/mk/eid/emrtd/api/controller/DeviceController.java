package vn.mk.eid.emrtd.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.mk.eid.emrtd.api.data.Devices;
import vn.mk.eid.emrtd.api.service.DeviceService;
import vn.mk.eid.emrtd.common.data.ResultCode;
import vn.mk.eid.emrtd.common.data.ServiceResult;
import vn.mk.eid.emrtd.common.web.BaseCardRequest;
import vn.mk.eid.emrtd.icao.service.EmrtdService;

@RestController
//@RequestMapping("/device")

public class DeviceController {
    EmrtdService emrtdService;
    @Autowired
    private DeviceService deviceService;

    @Autowired
    public void setEmrtdService(EmrtdService emrtdService) {
        this.emrtdService = emrtdService;
    }

    @GetMapping("/devices")
    public ServiceResult getDevices() {
        try {
            Devices response = new Devices();

            String samreader = deviceService.getSamreader();
            if (samreader.equals("notregistered"))
                return ServiceResult.fail(ResultCode.READER_NOT_REGISTERED);
            response.setReaders(deviceService.getReader());
            response.setCameras(deviceService.getCamera());
            response.setScanners(deviceService.getScanner());

            return ServiceResult.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ServiceResult.fail(ResultCode.FAILED_TO_GET_DEVICES_LIST);
    }


    @PostMapping("/checkCardPresent")
    public ServiceResult checkCardPresent(@RequestBody BaseCardRequest request) {
        return emrtdService.checkCardPresent(request);
    }

}
