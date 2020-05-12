package no.ssb.api.forbruk.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import no.ssb.api.forbruk.repository.SsbVetduatRestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.Files.*;

/**
 * Created by rsa on 29.04.2019.
 */
@Service
public class ForbrukService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    SsbVetduatRestRepository ssbVetduatRestRepository;


    final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-hh-mm-ss");
    public String retrieveProductInformation(String codefilesPath, String resultFileDir, String resultFilePrefix, String resultFilePostfix) {
        log.info("*** start retrieve product information ***");

        log.info("hent alle filer i {}", codefilesPath);

        log.info("løp gjennom alle filene");
        try(Stream<Path> walk = walk(Paths.get(codefilesPath))) {
            walk.filter(Files::isRegularFile)
                    .forEach(f -> {
                        Path resultFile = createProduktInfoFile(f.getFileName().toString(), resultFileDir, resultFilePrefix, resultFilePostfix);
                        ArrayList<String> fileCodeList = getCodesFromFile(f);
                        ArrayNode produkter = mapper.createArrayNode();
                        fileCodeList.stream().forEach(codes -> {
                            log.info("codes from file: {}", codes);
                            String produktInfo = ssbVetduatRestRepository.callSsbVetDuAt(codes);
                            try {
                                JsonNode produktListe = mapper.readTree(produktInfo);
                                if (produktListe.isArray()) {
                                    produktListe.forEach(p -> produkter.add(p));
                                    log.info("(a) antall produkt: {}", produkter.size());
                                } else {
                                    produkter.add(produktListe);
                                    log.info("(b) antall produkt: {}", produkter.size());
                                }
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                                log.error("Noe gikk galt under konvertering av " + produktInfo);
                            }
                        });
                        log.info("til fil - antall produkt: {}", produkter.size());
                        addToProduktInfoFile(produkter, resultFile);
                    });

        } catch (IOException ioe) {
            log.error("Something wrong reading files from {}, {}", codefilesPath, ioe.getMessage());
        }
        return "OK";
    }

    private Path createProduktInfoFile(String codefileName, String resultFileDir, String resultFilePrefix, String resultFilePostfix) {
        log.info("codefilenName: {}, resultFilePath: {}, resultFilePrefix: {}", codefileName, resultFileDir, resultFilePrefix);
        String trimmedCodefileName = codefileName.substring(0, codefileName.lastIndexOf('.'));
        Path resultFilePath = Paths.get(resultFileDir + resultFilePrefix + trimmedCodefileName + "_" + LocalDateTime.now().format(dateFormatter) + resultFilePostfix);
        log.info(resultFilePath.toString());
        try {
            Files.createFile(resultFilePath);
        } catch (IOException e) {
            log.error("Something wrong writing file {}, {}", resultFilePath.getFileName(), e.getMessage());
        }
        return resultFilePath;
    }

    private void addToProduktInfoFile(ArrayNode produktInfo, Path file) {
        log.info("file: {}, produktInfo: {}", file.toAbsolutePath(), produktInfo.toString());
        try {
            Files.writeString(file, produktInfo.toString(), StandardOpenOption.APPEND);
            log.info("filinnhold: {}", Files.readString(file));
        } catch (IOException e) {
            log.error("Something wrong writing file {}, {}", file.getFileName(), e.getMessage());
        }
    }

    private ArrayList<String> getCodesFromFile(Path f) {
        ArrayList<String> allCodes = new ArrayList<>();
        try (Stream<String> lines = Files.lines(f)) {
            lines.forEach(line -> {
                log.info("line: {}", line);
                String[] linecodes = line.replace(";", ",").split(",");
                Arrays.parallelSetAll(linecodes, (i) -> linecodes[i].trim());
                Arrays.asList(linecodes).stream().forEach(c -> log.info("  code: {}.", c));
                allCodes.add(Arrays.asList(linecodes).stream().collect(Collectors.joining(",")));
            });
            log.info("({}) allCodes: {}", f.getFileName().toString(), allCodes);
        } catch (IOException e) {
            log.error("error reading codes from {}", f);
        }
        return allCodes;
    }
}
