package vn.mk.eid.emrtd.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NFinger;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.client.NBiometricClient;
import com.neurotec.images.NImage;
import com.neurotec.licensing.NLicense;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.util.HtmlUtils;
import vn.mk.eid.emrtd.api.data.BiometricData;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ServerFingerWebsocketHandler extends BinaryWebSocketHandler implements SubProtocolCapable {

    private static final Logger logger = LoggerFactory.getLogger(ServerFingerWebsocketHandler.class);
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final BioMiniManager bio = new BioMiniManager();
    @Value("${biometric.quality.finger}")
    private Integer fingerQuality;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("Server connection opened");
        sessions.add(session);

        bio.init();
        bio.StartCapturing();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        logger.info("Server connection closed: {}", status);
        sessions.remove(session);
        // uninit
        bio.uninit();
    }

    @Scheduled(fixedRate = 50)
    void sendFingerImage() throws IOException {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                byte[] image = bio.pollRequest();
                if (image != null) {
                    BiometricData dataFinger = new BiometricData();
                    dataFinger.setType(2);
                    dataFinger.setImage(image);
                    dataFinger.setQuality(0);
                    ObjectMapper objectMapper = new ObjectMapper();
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(dataFinger)));
                }
            }
        }
    }

    @Scheduled(fixedRate = 500)
    public void checkFingerQuality() throws IOException {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                byte[] image = bio.pollRequest();
                if (image != null) {
                    BiometricData dataFinger = new BiometricData();
                    dataFinger.setType(2);
                    dataFinger.setImage(image);
                    if (!checkLicense()) {
                        throw new RuntimeException("Could not obtain SDK license");
                    }
                    NBiometricClient client = new NBiometricClient();
                    NSubject subject = new NSubject();
                    NFinger finger = null;
                    byte[] fingerTemplate = null;
                    try {
                        finger = new NFinger();
                        NImage nImage = NImage.fromMemory(ByteBuffer.wrap(image));
                        finger.setImage(nImage);
                        subject.getFingers().add(finger);
                        NBiometricStatus status = client.createTemplate(subject);
                        if (status == NBiometricStatus.OK) {
                            fingerTemplate = subject.getTemplateBuffer().toByteArray();
                            System.out.println("Template extracted.");
                            //set template
                            dataFinger.setTemplate(fingerTemplate);
                            int qualityFinger = 0;
                            for (NFinger nFinger : subject.getFingers()) {
                                qualityFinger = (int) nFinger.getObjects().get(0).getQuality();
                                //set quality
                                dataFinger.setQuality(qualityFinger);
                            }
                            //send object
                            if (session.isOpen()) {
                                ObjectMapper objectMapper = new ObjectMapper();

                                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(dataFinger)));
                                if (dataFinger.getQuality() >= fingerQuality) {
                                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(dataFinger)));
                                }
                            }
                        } else {
                            System.out.println("Extraction failed: " + status);
                        }
                    } finally {
                        if (finger != null) {
                            finger.dispose();
                        }
                        if (subject != null) {
                            subject.dispose();
                        }
                        if (client != null) {
                            client.dispose();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        ByteBuffer request = message.getPayload();
        logger.info("Server received: {}", request);

        String response = String.format("response from server to '%s'", HtmlUtils.htmlEscape(String.valueOf(request)));
        logger.info("Server sends: {}", response);
        session.sendMessage(new TextMessage(response));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.info("Server transport error: {}", exception.getMessage());
    }

    @Override
    public List<String> getSubProtocols() {
        return Collections.singletonList("subprotocol.demo.fingerwebsocket");
    }

    public boolean checkLicense() throws IOException {
        boolean licensePass = true;
        final String license = "FingerExtractor";
        if (!NLicense.obtain("/local", 5000, license)) {
//        if (!NLicense.obtain("14.248.80.2", 5000, license)) {
            System.err.format("Could not obtain license: %s%n", license);
            licensePass = false;
        }
        return licensePass;
    }
}
