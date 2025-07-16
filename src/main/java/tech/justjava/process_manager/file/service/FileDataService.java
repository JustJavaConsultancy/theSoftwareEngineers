package tech.justjava.process_manager.file.service;

import java.io.File;
import java.net.URLConnection;
import java.sql.Blob;
import java.time.Duration;
import java.util.UUID;
import javax.sql.rowset.serial.SerialBlob;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import tech.justjava.process_manager.file.domain.FileContent;
import tech.justjava.process_manager.file.model.FileData;
import tech.justjava.process_manager.file.repos.FileContentRepository;
import tech.justjava.process_manager.process.service.NotFoundException;


@Service
@Slf4j
public class FileDataService {

    private static final String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/uploads";

    private final FileContentRepository fileContentRepository;

    public FileDataService(final FileContentRepository fileContentRepository) {
        this.fileContentRepository = fileContentRepository;
    }

    private String encodeFileName(final String fileName) {
        return fileName.replaceAll("[^0-9a-zA-Z.!\\-_\\[\\]]", "-");
    }

    @SneakyThrows
    public FileData saveUpload(final MultipartFile uploadFile) {
        if (uploadFile.isEmpty()) {
            // no file submitted or no content
            return null;
        }

        log.info("saving uploaded file {}", uploadFile.getOriginalFilename());

        final String uid = UUID.randomUUID().toString();
        final String encodedFileName = encodeFileName(uploadFile.getOriginalFilename());
        final File tempDir = new File(UPLOAD_DIRECTORY + "/" + uid);
        if (!tempDir.mkdirs()) {
            throw new RuntimeException("could not prepare temporary directory " + tempDir.getPath());
        }
        final File tempFile = new File(tempDir, encodedFileName);
        uploadFile.transferTo(tempFile);

        final FileData fileData = new FileData();
        fileData.setUid(uid);
        fileData.setFileName(encodedFileName);
        return fileData;
    }

    @Transactional(
            propagation = Propagation.MANDATORY
    )
    @SneakyThrows
    public void persistUpload(final FileData fileData) {
        if (fileData == null) {
            return;
        }

        log.info("persisting file upload {}", fileData.getUid());

        final File tempFile = new File(UPLOAD_DIRECTORY + "/" + fileData.getUid() + "/" + fileData.getFileName());
        final FileContent fileContent = new FileContent();
        fileContent.setUid(fileData.getUid());
        fileContent.setContent(new SerialBlob(FileCopyUtils.copyToByteArray(tempFile)));
        fileContentRepository.save(fileContent);

        if (!tempFile.delete()) {
            log.error("could not delete file {}", tempFile.getPath());
        }
    }

    @Transactional(
            propagation = Propagation.MANDATORY
    )
    public void removeFileContent(final FileData fileData) {
        if (fileData != null) {
            fileContentRepository.deleteById(fileData.getUid());
        }
    }

    @Transactional(
            propagation = Propagation.MANDATORY
    )
    public void handleUpdate(final FileData oldFileData, final FileData newFileData) {
        if (oldFileData != null && newFileData != null && oldFileData.getUid().equals(newFileData.getUid())) {
            // no change
            return;
        }
        if (oldFileData != null) {
            removeFileContent(oldFileData);
        }
        if (newFileData != null) {
            persistUpload(newFileData);
        }
    }

    @Transactional
    @SneakyThrows
    public ResponseEntity<InputStreamResource> provideDownload(final FileData fileData) {
        if (fileData == null) {
            throw new NotFoundException();
        }
        final Blob fileContent = fileContentRepository.findById(fileData.getUid())
                .map(FileContent::getContent)
                .orElseThrow(NotFoundException::new);
        final InputStreamResource inputStreamResource = new InputStreamResource(fileContent.getBinaryStream());
        String contentType = URLConnection.guessContentTypeFromName(fileData.getFileName());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileData.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(fileContent.length())
                .body(inputStreamResource);
    }

    @Scheduled(cron = "0 0 0-23/2 * * *")
    public void cleanUploadDir() {
        log.info("cleaning upload dir");
        final File uploadDir = new File(UPLOAD_DIRECTORY);
        final File[] subDirs = uploadDir.listFiles();
        if (subDirs == null) {
            return;
        }
        final long cutoff = System.currentTimeMillis() - Duration.ofHours(1).toMillis();
        for (final File subDir : subDirs) {
            if (subDir.lastModified() < cutoff) {
                if (!FileSystemUtils.deleteRecursively(subDir)) {
                    log.error("could not delete directory {}", subDir.getPath());
                }
            }
        }
    }

}
