package com.wilo.server.media.service;

import com.wilo.server.files.service.FileService;
import com.wilo.server.global.exception.ApplicationException;
import com.wilo.server.media.dto.MediaUploadResponse;
import com.wilo.server.media.entity.Media;
import com.wilo.server.media.entity.MediaSourceType;
import com.wilo.server.media.entity.MediaType;
import com.wilo.server.media.exception.MediaErrorCase;
import com.wilo.server.media.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class MediaService {

    private final FileService fileService;
    private final MediaRepository mediaRepository;

    public MediaUploadResponse upload(MultipartFile file, String sourceType) {

        if (file == null || file.isEmpty()) {
            throw new ApplicationException(MediaErrorCase.INVALID_MEDIA_FILE);
        }

        String url = fileService.uploadFileToS3(file);

        MediaType mediaType = resolveMediaType(file.getContentType());

        MediaSourceType source;

        try {
            source = MediaSourceType.valueOf(sourceType);
        } catch (IllegalArgumentException e) {
            throw new ApplicationException(MediaErrorCase.INVALID_SOURCE_TYPE);
        }

        Media media = Media.create(
                url,
                null,
                mediaType,
                source
        );

        mediaRepository.save(media);

        return MediaUploadResponse.from(media);
    }

    private MediaType resolveMediaType(String contentType) {

        if (contentType == null) {
            throw new ApplicationException(MediaErrorCase.INVALID_MEDIA_FILE);
        }

        if (contentType.startsWith("image")) {
            return MediaType.IMAGE;
        }

        if (contentType.startsWith("audio")) {
            return MediaType.AUDIO;
        }

        throw new ApplicationException(MediaErrorCase.UNSUPPORTED_MEDIA_TYPE);
    }
}