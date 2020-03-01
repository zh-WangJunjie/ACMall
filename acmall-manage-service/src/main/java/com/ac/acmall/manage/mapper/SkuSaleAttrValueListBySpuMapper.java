package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SkuSaleAttrValueListBySpuMapper {

    List<PmsSkuSaleAttrValue> getSkuSaleAttrValueListBySpu(@Param("spuId") String spuId);
}
