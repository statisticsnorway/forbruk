package no.ssb.api.forbruk.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.ssb.api.forbruk.repository.SsbVetduatRestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import static java.nio.file.Files.walk;

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

    public String retrieveProductInformation(String codefilesPath, String generatedFileDir, String resultFileDir, String resultFilePrefix,
                                             String resultFilePostfix, String codeType, String removeElements, String handledFileDir) {
        Instant start = Instant.now();
        log.info("*** start retrieve product information ***");
        log.info("løp gjennom alle filene i {}", codefilesPath);
        log.info("fjern {}", removeElements);
        log.info("flytt filer til {}", handledFileDir);
        try(Stream<Path> walk = walk(Paths.get(codefilesPath))) {
            walk.filter(Files::isRegularFile)
                    .forEach(file -> {
                        treatFile(generatedFileDir, resultFileDir, resultFilePrefix, resultFilePostfix, file, codeType, removeElements);
                        moveHandledFile(file, handledFileDir);
                    });

        } catch (IOException ioe) {
            log.error("Something wrong reading files from {}, {}", codefilesPath, ioe.getMessage());
        }
        log.info("ferdig behandlet {} , brukt tid: {} millisek", codefilesPath, Duration.between(start, Instant.now()).toMillis());

        return "OK";
    }

    private void treatFile(String generateFileDir, String resultFileDir, String resultFilePrefix,
                           String resultFilePostfix, Path f, String codeType, String removeElements) {
        try {
            String resultFileName = createProduktInfoFileName(
                    f.getFileName().toString(), resultFilePrefix, resultFilePostfix);
            Path generateFile = createProduktInfoFile(resultFileName, generateFileDir);
            ArrayList<String> fileCodeList = getCodesFromFile(f);
            final ArrayNode[] produkter = {mapper.createArrayNode()};
//                        fileCodeList.parallelStream().forEach(codes -> {
            //parallelStream øker ikke hastighet
            fileCodeList.forEach(codes -> {
                log.info("codes from file: {}", codes);
                collectProductInformationForCodes(produkter[0], codeType, codes, removeElements);
//                if (produkter[0].size() > 1000) {
//                    log.info("til fil - antall produkt: {}", produkter[0].size());
//                    addToProduktInfoFile(produkter[0], generateFile);
//                    produkter[0] = mapper.createArrayNode();
//                }
            });
            log.info("til fil - antall produkt: {}", produkter[0].size());
            addToProduktInfoFile(produkter[0], generateFile);
            Path resultFile = createProduktInfoFile(resultFileName, resultFileDir);
            Files.move(generateFile, resultFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            log.error("Something went wrong handling file {}: {}", f.toString(), e.getMessage());
        }
    }

    private void collectProductInformationForCodes(ArrayNode produkter, String codeTypes, String codes, String removeElements) {
        String produktInfo = ssbVetduatRestRepository.callSsbVetDuAt(codeTypes, codes);
        try {
            JsonNode produktListe = mapper.readTree(produktInfo);
            if (produktListe.isArray()) {
                produktListe.forEach(p -> produkter.add(removeUnnecessary(p, removeElements)));
            } else {
                produkter.add(removeUnnecessary(produktListe, removeElements));
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            log.error("Noe gikk galt under konvertering av " + produktInfo);
        }
    }

    private JsonNode removeUnnecessary(JsonNode node, String removeElements) {
        if (removeElements != null && removeElements.length() > 0) {
            Arrays.asList(removeElements.split(",")).forEach(element -> {
                if (node.isArray() && !node.isEmpty()) {
                    node.forEach(n -> ((ObjectNode) n).remove(element));
                } else {
                    ((ObjectNode) node).remove(element);
                }
            });
        }
        return node;
    }

    private Path createProduktInfoFile(String fileName, String resultFileDir) {
        log.info("fileName: {}, resultFilePath: {}", fileName, resultFileDir);
        Path resultFilePath = Paths.get(resultFileDir + fileName);
        log.info(resultFilePath.toString());
        try {
            Files.createFile(resultFilePath);
        } catch (IOException e) {
            log.error("Something wrong creating file {}, {}", resultFilePath.getFileName(), e.getMessage());
        }
        return resultFilePath;
    }

    private String createProduktInfoFileName(String codefileName, String resultFilePrefix, String resultFilePostfix) {
        log.info("codefilenName: {}, resultFilePath: {}, resultFilePrefix: {}", codefileName, resultFilePrefix);
        String trimmedCodefileName = codefileName.substring(0, codefileName.lastIndexOf('.'));
        return resultFilePrefix + trimmedCodefileName + "_" + LocalDateTime.now().format(dateFormatter) + resultFilePostfix;
    }

    private void addToProduktInfoFile(ArrayNode produktInfo, Path file) {
        log.info("file: {}, produktInfo: {}", file.toAbsolutePath(), produktInfo.toString());
        try {
            Files.writeString(file, produktInfo.toString(), StandardOpenOption.APPEND);
            log.info("filinnhold: {}", Files.readString(file));
        } catch (IOException e) {
            log.error("Something wrong writing produktinfo to file {}, {}", file.getFileName(), e.getMessage());
        }
    }

    private ArrayList<String> getCodesFromFile(Path f) {
        ArrayList<String> allCodes = new ArrayList<>();
        try (Stream<String> lines = Files.lines(f)) {
            lines.forEach(line -> {
                log.info("line: {}", line);
                String[] linecodes = line.replace(";", ",").split(",");
                Arrays.parallelSetAll(linecodes, (i) -> linecodes[i].trim());
                Arrays.asList(linecodes).parallelStream().forEach(c -> log.info("  code: {}.", c));
                allCodes.add(String.join(",", linecodes));
            });
            log.info("({}) allCodes: {}", f.getFileName().toString(), allCodes);
        } catch (IOException e) {
            log.error("error reading codes from {}", f);
        }
        return allCodes;
    }

    private void moveHandledFile(Path file, String handledFileDir) {
        log.info("file: {}, handledDir: {} ({})", file, handledFileDir, file.getFileName().toString());
        String trimmedFileName = file.getFileName().toString().substring(0, file.getFileName().toString().lastIndexOf('.'));
        String filePostFix = file.getFileName().toString().substring(file.getFileName().toString().lastIndexOf('.'));
        Path movedFilePath = Paths.get(handledFileDir + trimmedFileName + "_" +
                LocalDateTime.now().format(dateFormatter) + filePostFix );
        log.info(movedFilePath.toString());
        try {
            Files.move(file, movedFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Something wrong moving file {}, {}", file.getFileName().toString(), e.getMessage());
        }

    }



}
