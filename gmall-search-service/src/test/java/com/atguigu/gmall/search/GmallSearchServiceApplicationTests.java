package com.atguigu.gmall.search;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.SpuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GmallSearchServiceApplicationTests {

    @Autowired
    JestClient jestClient;

    @Reference
    SpuService spuService;

    /**
     * 本测试方法为测试将数据库中所有的skuInfo
     * 转换成searchSkuInfo
     * PUT到es中
     * */
    @Test
    public void contextLoads() {
        //提取数据库中的skuInfo置于searchSkuInfo
        List<PmsSkuInfo> skuInfoList = spuService.getSkuForSearch();

        //声明一个PmsSearchSkuInfo集合容器
        List<PmsSearchSkuInfo> pmsSearchSkuInfoList = new ArrayList<>();

        //debug查看skuInfoList数据无误后，将skuInfoList里面的PmsSkuInfo复制到PmsSearchSkuInfo
        for (PmsSkuInfo pmsSkuInfo : skuInfoList) {
            //声明一个PmsSearchSkuInfo容器用于复制
            PmsSearchSkuInfo pmsSearchSkuInfo = new PmsSearchSkuInfo();
            //开始复制
            BeanUtils.copyProperties(pmsSkuInfo,pmsSearchSkuInfo);
            //核对pmsSkuInfo、pmsSearchSkuInfo后，发现里面pmsSearchSkuInfo的id是long类型的
            //把pmsSkuInfo中String类型的id转换成long类型
            pmsSearchSkuInfo.setId(Long.parseLong(pmsSkuInfo.getId()));
            //最后将pmsSearchSkuInfo装入list中
            pmsSearchSkuInfoList.add(pmsSearchSkuInfo);
        }

        //PUT到gmall0722中，遍历pmsSearchSkuInfoList一条一条地PUT
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfoList) {
            Index index = new Index.Builder(pmsSearchSkuInfo).index("gmall0722").type("PmsSearchSkuInfo").build();
            try {
                //执行
                jestClient.execute(index);
            } catch (Exception e) {

            } finally {

            }
        }
    }

}
