package vn.mk.eid.emrtd.api.service;

import com.neurotec.biometrics.client.NBiometricClient;
import com.neurotec.devices.NDevice;
import com.neurotec.devices.NDeviceManager;
import com.neurotec.devices.NDeviceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.mk.eid.emrtd.common.web.BaseCardRequest;
import vn.mk.eid.emrtd.icao.service.EmrtdService;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
public class DeviceService {

    EmrtdService emrtdService;
    @Value("${readerlist.readers}")
    private String readers;
    @Value("${readerlist.samreaders}")
    private String samreaders;
    @Value("${readerlist.cameras}")
    private String cameras;
    @Value("${readerlist.fingerscanners}")
    private String fingerscanners;

    @Autowired
    public void setEmrtdService(EmrtdService emrtdService) {
        this.emrtdService = emrtdService;
    }


    public String getReader() {
        try {
            List<String> readerList = emrtdService.getReaders().getData();

            String[] readerConfig = readers.split("\\|");
            String response;

            for (int i = 0; i < readerConfig.length; i++) {
                if (readerList.contains(readerConfig[i])) {
                    return readerConfig[i]; // return the first reader in the config file
                }
            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";//new ArrayList<>();
        }
    }

    public String getSamreader() {
        try {
            List<String> readerList = emrtdService.getReaders().getData();

            String[] samreaderConfig = samreaders.split("\\|");
            for (int i = 0; i < samreaderConfig.length; i++) {
                if (readerList.contains(samreaderConfig[i])) {
                    BaseCardRequest request = new BaseCardRequest();
                    request.setReader(samreaderConfig[i]);
                    boolean isCard = emrtdService.checkCardPresent(request).getSuccess();
                    if (isCard)
                        return samreaderConfig[i];
                    ;
                }
            }
            return "notregistered";
        } catch (Exception e) {
            e.printStackTrace();
            return "";//new ArrayList<>();
        }
    }

    public String getCamera() {
        try {
            NBiometricClient client = new NBiometricClient();
            client.setUseDeviceManager(true);
            NDeviceManager deviceManager = client.getDeviceManager();
            deviceManager.setDeviceTypes(EnumSet.of(NDeviceType.CAMERA));
            deviceManager.initialize();

            List<String> camera = new ArrayList<>();
            for (NDevice device : deviceManager.getDevices()) {
                camera.add(device.getDisplayName());
            }
            String[] webcamConfig = cameras.split("\\|");

            for (int i = 0; i < webcamConfig.length; i++) {
                if (camera.contains(webcamConfig[i])) {
                    return webcamConfig[i].concat(" 0");// return the first camera in the config file
                }
            }
//            List<Webcam> webcams = Webcam.getWebcams(1000L);
//            for (Webcam webcam : webcams
//            ) {
//                for (int i = 0; i < webcamConfig.length; i++) {
//                    String webcamname = webcam.getName();
//                    if (webcamname.equals(webcamConfig[i])) {
//                        return webcamConfig[i];// return the first camera in the config file
//                    }
//                }
//            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public String getScanner() {
        try {
            NBiometricClient client = new NBiometricClient();
            client.setUseDeviceManager(true);
            NDeviceManager deviceManager = client.getDeviceManager();
            deviceManager.setDeviceTypes(EnumSet.of(NDeviceType.FINGER_SCANNER));
            deviceManager.initialize();

            List<String> scanners = new ArrayList<>();
            for (NDevice device : deviceManager.getDevices()) {
                scanners.add(device.getDisplayName());
            }

            String[] scannerConfig = fingerscanners.split("\\|");

            for (int i = 0; i < scannerConfig.length; i++) {
                if (scanners.contains(scannerConfig[i])) {
                    return scannerConfig[i];// return the first finger scanner in the config file
                }
            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
