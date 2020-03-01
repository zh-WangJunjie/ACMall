package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {
    @Reference
    private SpuService spuService;

    @RequestMapping("spuSaleAttrValueJson1")
    @ResponseBody
    public String  spuSaleAttrValueJson(String spuId){
        //根据销售属性切换商品getSkuSaleAttrValueListBySpu
        List<PmsSkuInfo> skuInfoList = spuService.getSkuInfoListBySpu(spuId);
        HashMap<String,String> spuSaleAttrValueJSON = new HashMap<>();

        //把这个list封装成一个hash表
        for (PmsSkuInfo skuInfo : skuInfoList) {
            List<PmsSkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
            //将valueId串做为主键
            String valueIdKey = "";
            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
                String valueId = pmsSkuSaleAttrValue.getSaleAttrValueId();
                if (valueIdKey.length()!=0){
                    valueIdKey = valueIdKey + "|";
                }
                valueIdKey = valueIdKey + valueId;
            }
            //valueIdKey为键，skuId为值，封装到skuJSON中
            spuSaleAttrValueJSON.put(valueIdKey,skuInfo.getId());
        }


        //生成一份静态的json保存起来 D:\ideaLocalWorkspace\gmall\gmall-item-web\src\main\resources\static\spu
        String json = JSON.toJSONString(spuSaleAttrValueJSON);
        File file = new File("D:\\ideaLocalWorkspace\\gmall\\gmall-item-web\\src\\main\\resources\\static\\spu\\spu_"+spuId+".json");
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(json.getBytes());
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                fileOutputStream.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return json;
    }


    //spuSaleAttrListCheckBySku前台页面要求要传的对象
    @RequestMapping("{skuId}.html")
    public String getSkuInfo(@PathVariable("skuId") String skuId, Map<String ,Object> map){
        //根据skuId从后台调取数据skuInfo
        PmsSkuInfo pmsSkuInfo = spuService.getSkuInfo(skuId);


        //傻瓜方法
        List<PmsProductSaleAttr>  pmsProductSaleAttrList666 = spuService.spuSaleAttrListCheck(skuId,pmsSkuInfo.getSpuId());
        //根据skuId、spuId从后台调取数据spuSaleAttrListCheckBySku
        List<PmsProductSaleAttr>  pmsProductSaleAttrList = spuService.spuSaleAttrListCheckBySku(skuId,pmsSkuInfo.getSpuId());
        //往域里面传值
        map.put("skuInfo",pmsSkuInfo);
        map.put("spuSaleAttrListCheckBySku666",pmsProductSaleAttrList666);
        map.put("spuSaleAttrListCheckBySku",pmsProductSaleAttrList);


        //根据销售属性切换商品getSkuSaleAttrValueListBySpu
        List<PmsSkuInfo> skuInfoList = spuService.getSkuInfoListBySpu(pmsSkuInfo.getSpuId());
        HashMap<String,String> spuSaleAttrValueJSON = new HashMap<>();

        //把这个list封装成一个hash表
        for (PmsSkuInfo skuInfo : skuInfoList) {
            List<PmsSkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
            //将valueId串做为主键
            String valueIdKey = "";
            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
                String valueId = pmsSkuSaleAttrValue.getSaleAttrValueId();
                if (valueIdKey.length()!=0){
                    valueIdKey = valueIdKey + "|";
                }
                valueIdKey = valueIdKey + valueId;
            }
            //valueIdKey为键，skuId为值，封装到skuJSON中
            spuSaleAttrValueJSON.put(valueIdKey,skuInfo.getId());
        }

        //把map变成串
        String json = JSON.toJSONString(spuSaleAttrValueJSON);
        //放进域中
        map.put("json",json);
        map.put("spuId",pmsSkuInfo.getSpuId());
        //跳转到item页面
        return "item";
    }

}
