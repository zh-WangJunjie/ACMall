package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.PmsProductSaleAttr;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SpuSaleAttrListCheckBySkuMapper {
    List<PmsProductSaleAttr> selectSpuSaleAttrListCheckBySku(@Param("skuId") String skuId, @Param("spuId") String spuId);
}
