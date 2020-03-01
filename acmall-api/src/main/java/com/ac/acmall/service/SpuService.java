package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;

public interface SpuService {
    List<PmsProductInfo> spuList(String catalog3Id);

    List<PmsBaseSaleAttr> baseSaleAttrList();

    List<PmsProductSaleAttr> spuSaleAttrList(String spuId);

    void saveSpuInfo(PmsProductInfo pmsProductInfo);

    List<PmsProductImage> spuImageList(String spuId);

    void saveSkuInfo(PmsSkuInfo pmsSkuInfo);

    PmsSkuInfo getSkuInfo(String skuId);

    PmsSkuInfo getSkuInfoDB(String skuId);

    List<PmsProductSaleAttr> spuSaleAttrListCheckBySku(String skuId, String spuId);

    List<PmsProductSaleAttr> spuSaleAttrListCheck(String skuId, String spuId);

    List<PmsSkuInfo> getSkuInfoListBySpu(String spuId);

    List<PmsSkuInfo> getSkuForSearch();

    PmsSkuInfo getProductBySkuId(String productSkuId);
}
