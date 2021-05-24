package vn.mk.eid.emrtd.api;

import com.neurotec.licensing.NLicense;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Set;

//import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages = {"vn.mk.eid"})
@Slf4j

public class ApiApplication {

    public ApiApplication() {
        String jni = System.getProperty("jna.library.path");
        if (StringUtils.isEmpty(jni)) {
            System.setProperty("jna.library.path", "data/lib/native");
        }

//        try {
//            obtainComponents(new TreeSet<String>());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

    private void obtainComponents(Set<String> licenses) throws IOException {
//        log.debug("Start license initiation:[{}, {}]", licenseServerIp, licenseServerPort);
        licenses.add("FingerExtractor");
        boolean anyMatchingComponent = false;
        for (String matchingComponent : licenses) {
            matchingComponent = matchingComponent.trim();
            if (NLicense.obtain("192.168.0.175", "5000", matchingComponent)) {
                anyMatchingComponent = true;
            }
        }
        if (!anyMatchingComponent) {
            log.error("Could not obtain any matching license hotst: {}, port: {}");
        }
        log.info("Finish license initiation.");
    }
}
