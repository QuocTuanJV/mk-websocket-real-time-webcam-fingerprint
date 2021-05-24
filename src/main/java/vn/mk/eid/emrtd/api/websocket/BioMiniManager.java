package vn.mk.eid.emrtd.api.websocket;

import com.suprema.BioMiniSDK;
import lombok.NoArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Queue;

@Component
@NoArgsConstructor
public class BioMiniManager {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BioMiniManager.class);
    static Queue<byte[]> m_Queue = new LinkedList<>();
    private final BioMiniSDK p = new BioMiniSDK();

    public void init() {
        logger.info("BioMini init");
        int nRes;
        nRes = p.UFS_Init();

        if (nRes == p.UFS_OK) {

            logger.info("UFS_Init = UFS_OK");
        }

        // set classname, let Spring find it when we are capture using callback function
        p.UFS_SetClassName("vn.mk.eid.emrtd.api.websocket.BioMiniManager");
    }

    public void uninit() {
        int nRes;
        nRes = p.UFS_Uninit();
        if (nRes == p.UFS_OK) {
            logger.info("UFS_Uninit = UFS_OK");
        }
    }

    public long[] GetCurrentScannerHandle() {
        long[] hScanner = new long[1];
        int nRes;
        int[] nNumber = new int[1];

        nRes = p.UFS_GetScannerNumber(nNumber);

        if (nRes == 0) {
            if (nNumber[0] <= 0) {
                return null;
            }
        } else {
            return null;
        }

        nRes = p.UFS_GetScannerHandle(0, hScanner);

        if (nRes == 0) {
            logger.info("get handler ok");
            return hScanner;
        }

        return null;
    }

    public void StartCapturing() {

        logger.info("StartCapturing");
        long[] hScanner;

        // timeout for capturing. //default = 5000, set = 0 infinite
        int[] timeout = new int[1];

        hScanner = GetCurrentScannerHandle();

        if (hScanner != null) {
            //set timeout scanner
            p.UFS_SetParameter(hScanner[0], p.UFS_PARAM_TIMEOUT, timeout);
            p.UFS_StartCapturing(hScanner[0], "captureCallback");
        }
    }

    public void captureCallback(int bFingerOn, byte[] pImage, int nWidth, int nHeight, int nResolution) {
        int[] Resolution = new int[1];
        int[] Height = new int[1];
        int[] Width = new int[1];
        int[] DataSize = new int[1];
        long[] hScanner;
        hScanner = GetCurrentScannerHandle();
        p.UFS_GetCaptureImageBufferInfo(hScanner[0], Width, Height, Resolution);

        logger.info("width = {}, height = {}, resolution = {}", Width[0], Height[0], Resolution);
        DataSize[0] =  Width[0] * Height[0];

        byte[] pImageData = new byte[500 * 500];
//        p.UFS_GetCaptureImageBuffer(hScanner[0], pImageData);
        p.UFS_GetCaptureImageBufferToBMPImageBuffer(hScanner[0], pImageData, DataSize);
        m_Queue.add(pImageData);

        logger.info("size poll add= " + m_Queue.size());
    }

    public byte[] pollRequest() {

        logger.info("pollRequest queue size = " +  m_Queue.size());

        return m_Queue.poll();
    }
}


