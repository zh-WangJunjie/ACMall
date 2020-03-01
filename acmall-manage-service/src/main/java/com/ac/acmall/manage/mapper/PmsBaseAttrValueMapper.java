package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.PmsBaseAttrValue;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

public interface PmsBaseAttrValueMapper extends Mapper<PmsBaseAttrValue> {
    void deleteByAttrId(@Param("attrId") String attrId);
}
