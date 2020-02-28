package com.atguigu.gmall.manage;


import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
public class GmallManageWebApplicationTests {

    @Test
    public void contextLoads() {

    }

    @Test
    public void textFileUpload() throws IOException, MyException {
        String file = GmallManageWebApplicationTests.class.getClassLoader().getResource("tracker.properties").getPath();
        System.out.println(file);
        //全局配置
        ClientGlobal.init(file);
        //获得客户端
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer connection = trackerClient.getConnection();
        StorageClient storageClient = new StorageClient(connection,null);
        //上传文件
        String[] jpgs = storageClient.upload_appender_file("C:\\Users\\SGG\\Desktop\\456.jpg", "jpg", null);

        for (String jpg : jpgs) {
            System.out.println(jpg);
        }
    }

}
