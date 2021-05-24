package vn.mk.eid.emrtd.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sarxos.webcam.Webcam;
import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NFace;
import com.neurotec.biometrics.NLAttributes;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.client.NBiometricClient;
import com.neurotec.images.NImage;
import com.neurotec.licensing.NLicense;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.HtmlUtils;
import vn.mk.eid.emrtd.api.data.BiometricData;
import vn.mk.eid.emrtd.api.service.DeviceService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ServerFaceWebSocketHandler extends TextWebSocketHandler implements SubProtocolCapable {

    private static final Logger logger = LoggerFactory.getLogger(ServerFaceWebSocketHandler.class);

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    @Value("${biometric.license.host}")
    public String host;
    @Value("${biometric.license.port}")
    public String port;
    @Value("${biometric.quality.face}")
    private Integer faceQuality;

    @Autowired
    private DeviceService device;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("Server connection opened");
        sessions.add(session);

        TextMessage message = new TextMessage("one-time message from server");
        logger.info("Server sends: {}", message);

        Webcam webcam = Webcam.getWebcamByName(device.getCamera());
        webcam.setViewSize(new Dimension(640, 480));
        webcam.open();

//        session.sendMessage(message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        logger.info("Server connection closed: {}", status);
        sessions.remove(session);

        Webcam webcam = Webcam.getWebcamByName(device.getCamera());
        if (webcam.isOpen()) {
            webcam.close();
        }
    }

    // send face image every 50ms
    @Scheduled(fixedRate = 50, initialDelay = 100)
    void sendPeriodicMessages() throws IOException {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
//                String broadcast = "server send image message " + LocalTime.now();
//                logger.info("Server sends: {}", broadcast);

                Webcam webcam = Webcam.getWebcamByName(device.getCamera());
//                Webcam webcam = Webcam.getDefault();

                if (webcam.isOpen()) {
                    BufferedImage image = webcam.getImage();

                    if (image != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(image, "jpg", baos);

//                        logger.info("data length {}", baos.toByteArray().length);

                        BiometricData dataFace = new BiometricData();
                        dataFace.setType(1);
                        dataFace.setImage(baos.toByteArray());
                        dataFace.setQuality(0);

                        if (session.isOpen()) {
//                            session.sendMessage(new BinaryMessage(baos.toByteArray()));
                            ObjectMapper objectMapper = new ObjectMapper();
                            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(dataFace)));
                        }
                    }
                }
            }
        }
    }

    // check face quality every 500ms
    @Scheduled(fixedRate = 50, initialDelay = 100)
    void sendPeriodicMessages2() throws IOException {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
//                String broadcast = "server create template message " + LocalTime.now();
//                logger.info("Server sends: {}", broadcast);

                //get from config later
                Webcam webcam = Webcam.getWebcamByName(device.getCamera());

                if (webcam.isOpen()) {

                    // neurotech implement directly, update later
                    {
                        final String license = "FaceExtractor";
                        NBiometricClient biometricClient = null;
                        NSubject subject = null;
                        NFace face = null;

                        if (!NLicense.obtain(host, port, license)) {
                            logger.info("No License Found");
                            return;
                        }

                        biometricClient = new NBiometricClient();
                        subject = new NSubject();
                        face = new NFace();

                        BufferedImage image = webcam.getImage();

                        if (image != null) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(image, "jpg", baos);

                            face.setImage(NImage.fromMemory(ByteBuffer.wrap(baos.toByteArray())));

                            subject.getFaces().add(face);

                            NBiometricStatus status = biometricClient.createTemplate(subject);

                            if (status == NBiometricStatus.OK) {
                                logger.info("Extract ok");
                                NFace nFace = null;
                                if (subject.getFaces() != null && subject.getFaces().size() > 0) {
                                    if (subject.getFaces().size() > 1) {
                                        nFace = subject.getFaces().get(1);
                                    } else {
                                        nFace = subject.getFaces().get(0);
                                    }

                                    BiometricData dataFace = new BiometricData();
                                    dataFace.setType(1);
                                    dataFace.setImage(baos.toByteArray());
                                    dataFace.setTemplate(subject.getTemplateBuffer().toByteArray());

                                    for (NLAttributes nlAttributes :
                                            nFace.getObjects()) {
                                        dataFace.setQuality((int) nlAttributes.getQuality());
                                    }

                                    if (session.isOpen()) {
                                        ObjectMapper objectMapper = new ObjectMapper();
                                        if (dataFace.getQuality() >= faceQuality)
                                            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(dataFace)));
                                    }
                                }
                            } else {
                                logger.info("Extract failed");
                            }
                        }
                    }
                }
            }
        }
    }


//    @Override
//    public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
//        ByteBuffer request = message.getPayload();
//        logger.info("Server received: {}", request);
//
//        String response = String.format("response from server to '%s'", HtmlUtils.htmlEscape(String.valueOf(request)));
//        logger.info("Server sends: {}", response);
//        session.sendMessage(new TextMessage(response));
//    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String request = message.getPayload();
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
        return Collections.singletonList("subprotocol.demo.facewebsocket");
    }
}
