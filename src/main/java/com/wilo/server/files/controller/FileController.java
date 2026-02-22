package com.wilo.server.files.controller;

import com.wilo.server.files.service.FileService;
import com.wilo.server.global.response.CommonResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    @PostMapping("/image")
    public CommonResponse<String> uploadImageFile(
            @RequestParam("image") MultipartFile file
    ) {
        String imageUrl = fileService.uploadFileToS3(file);

        return CommonResponse.success(imageUrl);
    }

    @PostMapping("/images")
    public CommonResponse<List<String>> uploadMultipleImageFiles(
            @RequestParam("images") List<MultipartFile> files
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
