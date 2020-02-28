package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.bean.PmsBaseAttrValue;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrValueMapper;
import com.atguigu.gmall.service.AttrInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
public class AttrInfoServiceImpl implements AttrInfoService {

    @Autowired
    PmsBaseAttrInfoMapper pmsBaseAttrInfoMapper;

    @Autowired
    PmsBaseAttrValueMapper pmsBaseAttrValueMapper;

    @Override
    public List<PmsBaseAttrInfo> attrInfoList(String catalog3Id) {
        PmsBaseAttrInfo pmsBaseAttrInfo = new PmsBaseAttrInfo();
        pmsBaseAttrInfo.setCatalog3Id(catalog3Id);
        List<PmsBaseAttrInfo> pmsBaseAttrInfoList = pmsBaseAttrInfoMapper.select(pmsBaseAttrInfo);
        for (PmsBaseAttrInfo baseAttrInfo : pmsBaseAttrInfoList) {
            String attrInfoId = baseAttrInfo.getId();
            PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
            pmsBaseAttrValue.setAttrId(attrInfoId);
            List<PmsBaseAttrValue> pmsBaseAttrValueList = pmsBaseAttrValueMapper.select(pmsBaseAttrValue);
            baseAttrInfo.setAttrValueList(pmsBaseAttrValueList);
        }
        return pmsBaseAttrInfoList;
    }

    @Override
    public void saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo) {
        /**
         * 首先要考虑这个属性数据库里面是不是已经存在了，
         * 如果已经存在了，就删掉之前的，保存新写的
         *
         * 这个保存属性信息包含了属性名、属性值
         * 存属性名很简单，直接存就可以
         * 但是存属性值的时候属性值是一个集合
         * 所以存完属性名之后，调用当前属性的id、属性值集合
         * 遍历属性值集合，再根据属性的id，用循环进行存值
         */
        //有主键则为修改，无主键则为保存
        String id = pmsBaseAttrInfo.getId();
        if (!StringUtils.isEmpty(id)){
            PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
            pmsBaseAttrValue.setAttrId(id);
            pmsBaseAttrValueMapper.delete(pmsBaseAttrValue);
        } else {
            pmsBaseAttrInfoMapper.insertSelective(pmsBaseAttrInfo);
        }

        /*PmsBaseAttrInfo pmsBaseAttrInfo1 = pmsBaseAttrInfoMapper.selectOne(pmsBaseAttrInfo);
        if(pmsBaseAttrInfo1 != null){
            //说明已经存在这个属性了，先删掉
            pmsBaseAttrValueMapper.deleteByAttrId(pmsBaseAttrInfo.getId());
        }else {
            pmsBaseAttrInfoMapper.insertSelective(pmsBaseAttrInfo);
        }*/


        String attrInfoId = pmsBaseAttrInfo.getId();
        List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();

        for (PmsBaseAttrValue pmsBaseAttrValue:attrValueList) {
            pmsBaseAttrValue.setAttrId(attrInfoId);
            pmsBaseAttrValueMapper.insertSelective(pmsBaseAttrValue);
        }
    }

    @Override
    public List<PmsBaseAttrValue> getAttrValueList(String attrId) {
        PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
        pmsBaseAttrValue.setAttrId(attrId);
        List<PmsBaseAttrValue> pmsBaseAttrValueList = pmsBaseAttrValueMapper.select(pmsBaseAttrValue);
        return pmsBaseAttrValueList;
    }

    /**
     *
     * @param valueIdSet
     * @return List<PmsBaseAttrInfo>
     * 本方法用于根据搜索结果提取的属性值集合valueIdSet，查询数据库，并返回平台属性值列表
     */
    @Override
    public List<PmsBaseAttrInfo> getAttrValueListByValueIdSet(Set<String> valueIdSet) {
        //两种方案
        //方案一：利用mybatis中的xml动态sql foreach进行查询
        //方案二：先在java中将这个id集合解析为一个字符串“1,2,3,4,5”，再传入数据库进行查询

        //这里用方案二，先将id集合解析为字符串
        String valueIds = StringUtils.collectionToDelimitedString(valueIdSet, ",");

        List<PmsBaseAttrInfo> pmsBaseAttrInfoList = pmsBaseAttrInfoMapper.getAttrValueListByValueIdSet(valueIds);

        return pmsBaseAttrInfoList;
    }


}
