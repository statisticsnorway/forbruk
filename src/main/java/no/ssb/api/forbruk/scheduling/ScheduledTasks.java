package no.ssb.api.forbruk.scheduling;

import no.ssb.api.forbruk.service.ForbrukService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Created by lrb on 07.02.2017.
 */
@Service
public class ScheduledTasks {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${forbruk.filepath.codes}")
    private String productCodesFilePath;

    @Value("${forbruk.filepath.result}")
    private String resultFilePath;

    @Value("${forbruk.filepath.result.fileprefix}")
    private String resultFilePrefix;

    @Value("${forbruk.filepath.result.filepostfix}")
    private String resultFilePostfix;

    @Value("${resultat.fjern.element}")
    String removeElements;

    @Value("${forbruk.filepath.handled}")
    String handledFileDir;

    @Value("${vetduat.default.codeType}")
    private String defaultCodeType;

    @Autowired
    ForbrukService forbrukService;

    @Scheduled(cron = "${scheduled.cron.filsjekk}")
    public void runProductInfo() {
        forbrukService.retrieveProductInformation(productCodesFilePath, resultFilePath, resultFilePrefix,
                resultFilePostfix, defaultCodeType, removeElements, handledFileDir);
    }


}
