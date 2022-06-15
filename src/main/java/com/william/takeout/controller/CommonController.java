package com.william.takeout.controller;

import com.william.takeout.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/common")
@Slf4j
public class CommonController {

    @Value("${upload.filePath}")//文件的路径放到application.yml里，到时候改配置文件就好了
    private String basePath;

    // 文件上传，http://localhost:8080/backend/page/demo/upload.html 这个页面提交图片
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file){
        // file是一个临时文件，需要转存到磁盘中的某个指定位置，否则本次请求完成后，临时文件file会删除
        //   upload方法名中的参数名 必须是file（文件上传表单的 中name属性值必须是file,name="file"）
        log.info("上传的文件为: "+file.toString());

        // 原始文件名
        String originFileName = file.getOriginalFilename();   // 假设为 abc.jpg
        String suffix = originFileName.substring(originFileName.lastIndexOf("."));  // suffix = .jpg

        // 使用UUID重新生成文件名，防止文件名重复，造成后面上传的文件覆盖前面上传的文件
        String fileName = UUID.randomUUID().toString() + suffix; // dasdfjksa.jpg

        // 创建一个目录takeout
        File dir = new File(basePath);
        if (!dir.exists()){
            dir.mkdirs();
        }

        try {
            // 将临时文件转存到指定位置: 指定位置通过配置文件导入（动态）；filename也是动态的
            file.transferTo(new File(basePath + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Result.success(fileName);//需要给页面返回文件名称
    }

    // 文件下载  download
    @GetMapping("/download")  //download?name...会提交一个name过来；输出流通过response来获得（响应）
    public void download(String name, HttpServletResponse response)  {

        try {
            // 通过输入流来读取文件内容(文件通过上面的upload方法保存在basePath里)
            FileInputStream fileInputStream = new FileInputStream(new File(basePath +name));

            //  通过输出流将文件写回到浏览器，并在浏览器展示图片
            ServletOutputStream outputStream = response.getOutputStream();
            response.setContentType("image/jpeg");
            int len = 0;
            byte[] bytes = new byte[1024];
            // 输入流读取到 内容放到 bytes数组中
            while((len = fileInputStream.read(bytes)) != -1){ // 输入流还没有读取完数据
                outputStream.write(bytes,0,len);
                outputStream.flush();
            }

            fileInputStream.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}