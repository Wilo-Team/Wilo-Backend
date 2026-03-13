package com.wilo.server.files.controller;

import com.wilo.server.files.service.FileService;
import com.wilo.server.global.response.CommonResponse;
import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<String> uploadImageFile(
            @Parameter(description = "업로드할 이미지", required = true,
                    schema = @Schema(type = "string", format = "binary"))
            @RequestPart("image") MultipartFile file
    ) {
        String imageUrl = fileService.uploadFileToS3(file);
        return CommonResponse.success(imageUrl);
    }


    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<List<String>> uploadMultipleImageFiles(
            @Parameter(description = "업로드할 이미지들", required = true,
                    schema = @Schema(type = "string", format = "binary"))
            @RequestPart("images") List<MultipartFile> files
    ) {
        List<String> imageUrls = files.stream()
                .map(fileService::uploadFileToS3)
                .toList();

        return CommonResponse.success(imageUrls);
    }


    @DeleteMapping("/image")
    public CommonResponse<String> deleteImageFile(
            @RequestParam("imageUrl") String imageUrl
    ) {
        fileService.deleteImageFileFromS3(imageUrl);
        return CommonResponse.success("이미지 삭제가 완료되었습니다.");
    }
}