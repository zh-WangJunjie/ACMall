package com.atguigu.gmall.util;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class MyFileUpload {


    public static String uploadFile(MultipartFile multipartFile) {

        String url = "http://192.168.126.129";



        //全局配置
        try {
            String trackerPath = MyFileUpload.class.getClassLoader().getResource("tracker.properties").getPath();

            ClientGlobal.init(trackerPath);

            //获得客户端
            TrackerClient trackerClient = new TrackerClient();

            TrackerServer connection = trackerClient.getConnection();

            StorageClient storageClient = new StorageClient(connection,null);

            /*上传文件*/
            //获取源文件
            String originalFilename = multipartFile.getOriginalFilename();
            //获取源文件后缀标识符“.”的坐标
            int i = originalFilename.indexOf(".");
            //根据坐标截取源文件后缀名
            String fileSuffix = originalFilename.substring(i + 1);
            //上传文件,并返回一个url字符串数组
            String[] paths = storageClient.upload_file(multipartFile.getBytes(), fileSuffix, null);
            //将这些字符串数组拼接成一个完整的url
            for (String path : paths) {
                url = url + "/" + path;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return url;

    }
}
