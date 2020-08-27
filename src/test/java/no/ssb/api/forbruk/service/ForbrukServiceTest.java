package no.ssb.api.forbruk.service;

import no.ssb.api.forbruk.repository.SsbVetduatRestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
@SpringBootTest
public class ForbrukServiceTest {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final String parentFileDir = "src/test/resources/files/";

    @Mock
    private SsbVetduatRestRepository ssbVetduatRestRepository;

    @InjectMocks
    ForbrukService forbrukService = new ForbrukService();

    @Value("${forbruk.filepath.codes}")
    private String productCodesFilePath;

    @Value("${forbruk.filepath.generate}")
    private String generateFilePath;

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

    @BeforeEach
    void cleanDirectories() {
        try {
            log.info("filer i config: {}", numberOfFilesInDir("config"));
            deleteFilesInDirectory("config");
            deleteFilesInDirectory("handled");
            deleteFilesInDirectory("result");
        } catch (IOException e) {
            log.error("Error cleaning handled-directory");
            e.printStackTrace();
        }
    }


    @Test
    void testForbrukService_testOneFile() {
        try {
            assertEquals(0, numberOfFilesInDir("config"));
            assertEquals(0, numberOfFilesInDir("handled"));
            assertEquals(0, numberOfFilesInDir("result"));

            copyFile("codes1.csv");
            assertEquals(1, numberOfFilesInDir("config"));

            when(ssbVetduatRestRepository.callSsbVetDuAt(anyString(), anyString())).thenReturn(readResponseFromFile("response1.json"));
            String testResult = forbrukService.retrieveProductInformation(productCodesFilePath, generateFilePath,
                    resultFilePath, resultFilePrefix, resultFilePostfix, "gtin", removeElements, handledFileDir);
            log.info("testResult: {}", testResult);
            assertEquals("OK", testResult);

            assertEquals(0, numberOfFilesInDir("config"));
            assertEquals(1, numberOfFilesInDir("handled"));
            assertEquals(1, numberOfFilesInDir("result"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    void testForbrukService_testTwoFiles() {
        try {
            assertEquals(0, numberOfFilesInDir("config"));
            assertEquals(0, numberOfFilesInDir("handled"));
            assertEquals(0, numberOfFilesInDir("result"));

            copyFile("codes1.csv");
            copyFile("codes2.csv");
            assertEquals(2, numberOfFilesInDir("config"));

            when(ssbVetduatRestRepository.callSsbVetDuAt(anyString(), anyString())).thenReturn(readResponseFromFile("response1.json"));
            String testResult = forbrukService.retrieveProductInformation(productCodesFilePath, generateFilePath,
                    resultFilePath, resultFilePrefix,
                    resultFilePostfix, "gtin", removeElements, handledFileDir);
            log.info("testResult: {}", testResult);
            assertEquals("OK", testResult);

            assertEquals(0, numberOfFilesInDir("config"));
            assertEquals(2, numberOfFilesInDir("handled"));
            assertEquals(2, numberOfFilesInDir("result"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private int numberOfFilesInDir(String dir) throws IOException {
        int numberOfFiles = 0;
        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
                Paths.get(parentFileDir + dir));
        for (Path path: directoryStream) {
            numberOfFiles += 1;
        }
        return numberOfFiles;
    }


    private void copyFile(String fileName) throws IOException {
        log.info("file: {}, ", fileName);
        Files.copy(Path.of(parentFileDir + "testfiles/" + fileName),
                Path.of(parentFileDir + "config/" + fileName),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private void deleteFilesInDirectory(String dir) throws IOException {
        Files.walk(Paths.get(parentFileDir + dir)).map(Path::toFile).forEach(f -> {
            if (!f.isDirectory()) {
                f.delete();
            }
        });
    }

    private String readResponseFromFile(String fileName) throws IOException {
        return Files.readString(Path.of(parentFileDir + "response/" + fileName));
    }

}
