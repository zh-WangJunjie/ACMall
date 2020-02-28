package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.AttrInfoService;
import com.atguigu.gmall.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;


@Controller
public class SearchController {
    @Reference
    private SearchService searchService;

    @Reference
    private AttrInfoService attrInfoService;

    //list.html?catalog3Id=61
    //当点击平台属性的时候，要跳转的页面
    //这个返回值应该是根据过滤条件进行筛选的，也即是搜索出来的结果
    //分析京东商城的业务逻辑，
    //发现并照样设计出PmsSearchParam传递页面参数，
    //设计出PmsSearchSkuInfo接收搜索出来的结果集
    //最后将页面想要的list传递过去
    @RequestMapping("list.html")
    public String search(PmsSearchParam pmsSearchParam, ModelMap modelMap){
        //根据pmsSearchParam(catalog3Id、keyword、valueId)去es中查结果
        //这个结果应该是一个PmsSearchSkuInfo的集合
        List<PmsSearchSkuInfo> searchSkuInfoList = searchService.search(pmsSearchParam);

        /**
         * 平台属性展示属性值列表
         * 展示的一定是从搜索结果中的sku中的属性值
         * 所以，要先把这些属性值从结果中提取出来，放在一个集合中
         * 然后根据这个id集合去数据库中调取数据
         * */
        //属性列表展示：1、将搜索出来的结果sku中的属性值去重抽取出来后，放入一个集合里面
        //去重首先想到的是set，它具有不可重复性
        Set<String> valueIdSet = new HashSet<>();
        //提取属性值的id，首先得排除一下null值可能性
        if (searchSkuInfoList != null && searchSkuInfoList.size()>0){
            for (PmsSearchSkuInfo pmsSearchSkuInfo : searchSkuInfoList) {
                //提取属性集合，并排除null值可能性
                List<PmsSkuAttrValue> skuAttrValueList = pmsSearchSkuInfo.getSkuAttrValueList();
                if (skuAttrValueList != null && skuAttrValueList.size()>0) {
                    for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                        //提取属性值id
                        String valueId = pmsSkuAttrValue.getValueId();
                        //塞进valueIdSet集合
                        valueIdSet.add(valueId);
                    }
                }
            }
        }

        //属性列表展示：2、根据这个valueIdSet去数据库中查询出平台属性已经属性值列表
        List<PmsBaseAttrInfo> pmsBaseAttrInfoList = null;
        if (valueIdSet != null && valueIdSet.size()>0){
            pmsBaseAttrInfoList = attrInfoService.getAttrValueListByValueIdSet(valueIdSet);
        }

        //面包屑功能
        //面包屑：1、提取请求参数pmsSearchParam的属性值
        String[] valueIds = pmsSearchParam.getValueId();
        //排除null值可能性
        if (valueIds != null && valueIds.length>0){
            //查看页面，发现面包屑想要的对象是attrValueSelectedList，一个面包屑的集合
            //所以，实现面包屑功能，先声明一个面包屑集合容器
            List<PmsSearchCrumb> searchCrumbs = new ArrayList<>();
            //面包屑包含 valueId  valueName  urlParam三个参数，先传valueId  urlParam
            for (String valueId : valueIds) {
                //声明一个面包屑容器
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                //实现拼接面包屑url的方法
                String urlParam = getUrlParam(pmsSearchParam,valueId);
                //将urlParam设置到面包屑
                pmsSearchCrumb.setUrlParam(urlParam);
                //将valueId设置到面包屑
                pmsSearchCrumb.setValueId(valueId);

                //思考：每点击一下平台属性值，就会新加一个面包屑，
                //但同时要删除当前平台属性值所对应的平台属性,
                //也即是，当当前属性id = 迭代器迭代的平台属性的id 时，执行remove
                //注意，迭代器也是游标，不可逆，所以一次循环都必须要先声明一个迭代器
                Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfoList.iterator();
                while (iterator.hasNext()) {
                    //提取pmsBaseAttrInfo
                    PmsBaseAttrInfo pmsBaseAttrInfo = iterator.next();
                    //提取attrValueList
                    List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
                    //遍历attrValueList，并判断  当前的valueId = pmsBaseAttrValue的id?
                    for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                        if (valueId.equals(pmsBaseAttrValue.getId())) {
                            //先提取valueName设置到面包屑里面
                            String valueName = pmsBaseAttrValue.getValueName();
                            pmsSearchCrumb.setValueName(valueName);
                            //如果相等，则移除当前游标所指的平台属性
                            iterator.remove();
                        }
                    }
                }
                //将面包屑塞到面包屑集合里面
                searchCrumbs.add(pmsSearchCrumb);
            }
            //将面包屑容器传给页面attrValueSelectedList
            modelMap.put("attrValueSelectedList",searchCrumbs);
        }



        //传回页面，先排除null值可能性，再传回页面
        if (pmsBaseAttrInfoList != null && pmsBaseAttrInfoList.size()>0){
            modelMap.put("attrList",pmsBaseAttrInfoList);
        }

        //观察页面发现每一个属性都是一个超链接
        //th:href="'/list.html?'+${urlParam}+'&valueId='+${attrValue.id}"
        //而且这个urlParam还是后台传过去的，所以要先拼接一下这个urlParam
        //将页面传来的pmsSearchParam解析成urlParam
        String urlParam = getUrlParam(pmsSearchParam);
        //将拼接好的urlParam传给页面
        modelMap.put("urlParam",urlParam);


        //查看网页想要的list对象 skuLsInfoList，并将这个list传回
        modelMap.put("skuLsInfoList",searchSkuInfoList);
        return "list";

    }

    /**
     *
     * @param pmsSearchParam
     * @return urlParam
     * 本方法用于解析pmsSearchParam成一个urlParam字符串
     */
    private String getUrlParam(PmsSearchParam pmsSearchParam,String... valueIdForCrumb) {
        //初始化urlParam
        String urlParam = "";

        //先提取pmsSearchParam里面的元素
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String keyword = pmsSearchParam.getKeyword();
        String[] valueIds = pmsSearchParam.getValueId();

        //先排除null值可能性，再对含有catalog3Id的urlParam进行拼接
        if (!StringUtils.isEmpty(catalog3Id)){
            //如果urlParam不为空则说明catalog3Id这个参数不是第一个参数，需要在传来的urlParam后面加个"&"
            if (!StringUtils.isEmpty(urlParam)){
                urlParam += "&";
            }
            //拼接带有catalog3Id参数的urlParam
            urlParam += "catalog3Id=" + catalog3Id;
        }

        //先排除null值可能性，再对含有keyword的urlParam进行拼接
        if (!StringUtils.isEmpty(keyword)){
            //如果urlParam不为空则说明keyword这个参数不是第一个参数，需要在传来的urlParam后面加个"&"
            if (!StringUtils.isEmpty(urlParam)){
                urlParam += "$";
            }
            //拼接带有keyword参数的urlParam
            urlParam += "keyword=" + keyword;
        }

        //先排除null值可能性，再对含有valueId的urlParam进行拼接
        if (valueIds != null && valueIds.length>0) {
            //这里不用判断urlParam，因为能到这里，说明urlParam一定不为空
            //由于面包屑的url与平台属性值的url功能类似，且参数相同，所以整合到一个方法里面
            //面包屑的url地址 = 当前url - 面包屑的valueId
            //拼接面包屑url之前，先排除null值可能性
            if (valueIdForCrumb != null && valueIdForCrumb.length>0){
                //面包屑不为空，说明当前请求是面包屑url
                for (String valueId : valueIds) {
                    //判断面包屑的valueId 与 当前要拼接的valueId，相等即不传
                    if (!valueId.equals(valueIdForCrumb[0])){
                        urlParam += "&valueId=" + valueId;
                    }
                }
            } else {
                //面包屑为空，说明没传这个值，说明当前请求是平台属性值的url
                //平台属性值的url地址 = 当前url + 当前属性值的valueId
                //遍历valueIds，并拼接带有valueId参数的urlParam
                for (String valueId : valueIds) {
                    urlParam += "&valueId=" + valueId;
                }
            }
        }
        return urlParam;
    }
}
