package vn.mk.eid.emrtd.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import vn.mk.eid.emrtd.common.data.ResultCode;
import vn.mk.eid.emrtd.common.data.ServiceResult;
import vn.mk.eid.emrtd.common.web.BaseCardRequest;
import vn.mk.eid.emrtd.common.web.ReadInfoRequest;
import vn.mk.eid.emrtd.icao.service.EmrtdService;

import javax.smartcardio.CardException;
import java.io.IOException;

@RestController
@RequestMapping("/card")
public class CardController {
    EmrtdService emrtdService;

    @Autowired
    public void setEmrtdService(EmrtdService emrtdService) {
        this.emrtdService = emrtdService;
    }

    @GetMapping("/readers")
    public ServiceResult getReaders() {
        return emrtdService.getReaders();
    }

    @PostMapping("/readInfo")
    public ServiceResult getInfo(@RequestBody ReadInfoRequest request) {
        return emrtdService.readInfo(request);
    }

    @PostMapping("/readeac")
    public ServiceResult getEAC(@RequestBody ReadInfoRequest request) {
        return emrtdService.readEAC(request);
    }

    @PostMapping("/checkCardPresent")
    public ServiceResult checkCardPresent(@RequestBody BaseCardRequest request) {
        return emrtdService.checkCardPresent(request);
    }

    @GetMapping("/chipid")
    public ServiceResult getChipId(@RequestParam(value = "reader") String reader) {
        try {
            BaseCardRequest request = new BaseCardRequest();
            request.setReader(reader);
            return emrtdService.getChipId(request);
        } catch (CardException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ServiceResult.fail(ResultCode.UNKNOWN_ERROR);
    }

}
